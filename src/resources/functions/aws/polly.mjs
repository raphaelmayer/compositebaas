import { PollyClient, SynthesizeSpeechCommand } from "@aws-sdk/client-polly";
import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";

const polly = new PollyClient();
const s3 = new S3Client();

async function uploadToS3(bucket, key, stream, contentType) {
    const putObjectParams = {
        Bucket: bucket,
        Key: key,
        Body: stream,
        ContentType: contentType,
    };
    await s3.send(new PutObjectCommand(putObjectParams));
}

async function getFileContent(bucket, fileName) {
    const getObjectParams = {
        Bucket: bucket,
        Key: fileName,
    };
    const getObjectCommand = new GetObjectCommand(getObjectParams);
    const s3Object = await s3.send(getObjectCommand);

    const streamToString = (stream) =>
        new Promise((resolve, reject) => {
            const chunks = [];
            stream.on("data", (chunk) => chunks.push(chunk));
            stream.on("error", reject);
            stream.on("end", () => resolve(Buffer.concat(chunks).toString("utf-8")));
        });

    return await streamToString(s3Object.Body);
}

export const handler = async (event) => {
    try {
        const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
        const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
        const inputBucket = body.inputBucket;
        const outputBucket = body.outputBucket || inputBucket;
        const voiceId = body.voiceId || "Joanna";
        const inputLanguage = convertLanguageCode(body.inputLanguage) || "en-US";
        const format = body.format || "mp3"; // output format
        const outputKeys = [];

        for (const fileName of fileNames) {
            const textContent = await getFileContent(inputBucket, fileName);

            // Synthesize the speech with AWS Polly
            const pollyParams = {
                Text: textContent,
                OutputFormat: format,
                VoiceId: voiceId,
                LanguageCode: inputLanguage,
            };

            const synthesizeSpeechCommand = new SynthesizeSpeechCommand(pollyParams);
            const pollyResult = await polly.send(synthesizeSpeechCommand);
            console.log(`Speech synthesized successfully for file: ${fileName}`);

            // Upload the audio to S3
            const outputKey = `${fileName.replace(".txt", "")}-synthesized-${Date.now()}.${format}`;
            const audioStream = await pollyResult.AudioStream.transformToByteArray();
            await uploadToS3(outputBucket, outputKey, Buffer.from(audioStream), `audio/${format}`);
            console.log(`Audio file uploaded to S3: ${outputKey}`);

            outputKeys.push(outputKey);
        }

        return {
            statusCode: 200,
            body: JSON.stringify({
                fileNames: outputKeys,
            }),
        };
    } catch (error) {
        console.error("Error during Polly synthesis or S3 upload:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};

function convertLanguageCode(code) {
    const languageMap = {
        "arb": "arb", // Arabic
        "ar": "ar-AE", // Arabic (Gulf)
        "ca": "ca-ES", // Catalan
        "yue": "yue-CN", // Chinese (Cantonese)
        "cmn": "cmn-CN", // Chinese (Mandarin)
        "cs": "cs-CZ", // Czech
        "da": "da-DK", // Danish
        "nl": "nl-NL", // Dutch (Netherlands)
        "en": "en-US", // English (US)
        "fi": "fi-FI", // Finnish
        "fr": "fr-FR", // French
        "hi": "hi-IN", // Hindi
        "de": "de-DE", // German
        "is": "is-IS", // Icelandic
        "it": "it-IT", // Italian
        "ja": "ja-JP", // Japanese
        "ko": "ko-KR", // Korean
        "nb": "nb-NO", // Norwegian
        "pl": "pl-PL", // Polish
        "pt": "pt-PT", // Portuguese (European)
        "ro": "ro-RO", // Romanian
        "ru": "ru-RU", // Russian
        "es": "es-ES", // Spanish (European)
        "sv": "sv-SE", // Swedish
        "tr": "tr-TR", // Turkish
        "cy": "cy-GB" // Welsh
    };

    // Check if the code is already in the full xx-XX format
    if (code.match(/^[a-z]{2,3}(-[A-Z]{2,3})?$/)) {
        return code;
    }

    // Otherwise, return the mapped code or undefined
    return languageMap[code] || undefined;
}