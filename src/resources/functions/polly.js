import { PollyClient, SynthesizeSpeechCommand } from "@aws-sdk/client-polly";
import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";

const polly = new PollyClient();
const s3 = new S3Client();

async function uploadToS3(bucket, key, stream, contentType) {
    const putObjectParams = {
        Bucket: bucket,
        Key: key,
        Body: stream,
        ContentType: contentType
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
            stream.on('data', (chunk) => chunks.push(chunk));
            stream.on('error', reject);
            stream.on('end', () => resolve(Buffer.concat(chunks).toString('utf-8')));
        });

    return await streamToString(s3Object.Body);
}

export const handler = async (event) => {
    const fileNames = event.fileNames;  
    const inputBucket = event.inputBucket;
    const outputBucket = event.outputBucket || inputBucket;
    const voiceId = event.voiceId || "Joanna";
    const inputLanguage = event.inputLanguage || "en-US";
    const format = event.format || "mp3"; // output format
    const outputKeys = [];

    try {
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
            const outputKey = `${fileName.replace('.txt', '')}-synthesized-${Date.now()}.${format}`;
            const audioStream = await pollyResult.AudioStream.transformToByteArray();
            await uploadToS3(outputBucket, outputKey, Buffer.from(audioStream), `audio/${format}`);
            console.log(`Audio file uploaded to S3: ${outputKey}`);

            outputKeys.push(outputKey);
        }

        return {
            fileNames: outputKeys
        };

    } catch (error) {
        console.error("Error during Polly synthesis or S3 upload:", error);
        return { error: error.message };
    }
};
