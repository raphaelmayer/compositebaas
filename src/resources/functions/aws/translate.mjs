import { TranslateClient, TranslateTextCommand } from "@aws-sdk/client-translate";
import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";

const s3Client = new S3Client();
const translateClient = new TranslateClient();

async function saveInS3(data, bucket, key) {
    const params = {
        Body: data,
        Bucket: bucket,
        Key: key,
    };
    await s3Client.send(new PutObjectCommand(params));
    console.log(`Saved file "${key}" in bucket "${bucket}"`);
}

async function loadFromS3(bucket, key) {
    return new Promise(async (resolve, reject) => {
        const params = {
            Bucket: bucket,
            Key: key,
        };
        const command = new GetObjectCommand(params);
        try {
            const response = await s3Client.send(command);
            let responseDataChunks = [];
            response.Body.once("error", (err) => reject(err));
            response.Body.on("data", (chunk) => responseDataChunks.push(chunk));
            response.Body.once("end", () => resolve(responseDataChunks.join("")));
        } catch (err) {
            return reject(err);
        }
    });
}

// Helper function to parse the file name (remove extension)
function parseFileName(fileName) {
    const lastDotIndex = fileName.lastIndexOf(".");
    const name = fileName.substring(0, lastDotIndex);
    return name;
}

export const handler = async (event) => {
    try {
        const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
        const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
        const inputBucket = body.inputBucket;
        const outputBucket = body.outputBucket || inputBucket;
        const inputLanguage = body.inputLanguage || "auto";
        const outputLanguage = body.outputLanguage || "en-US";
        const outputKeys = [];

        for (const fileName of fileNames) {
            const inputText = await loadFromS3(inputBucket, fileName);
            console.log(`Loaded file from S3: ${fileName}`);

            // Translate the text using AWS Translate
            const translateParams = {
                Text: inputText,
                SourceLanguageCode: inputLanguage,
                TargetLanguageCode: outputLanguage,
                Settings: {
                    Formality: "FORMAL", // Change to "INFORMAL" if needed
                    Profanity: "MASK",
                    Brevity: "ON",
                },
            };
            const command = new TranslateTextCommand(translateParams);
            const response = await translateClient.send(command);
            const translatedText = response.TranslatedText;
            console.log(`Text translated for file: ${fileName}`);

            // Save the translated text to S3
            const outputKey = `${parseFileName(fileName)}-translated-${Date.now()}.txt`;
            await saveInS3(translatedText, outputBucket, outputKey);
            console.log(`Translated text saved to S3: ${outputKey}`);

            outputKeys.push(outputKey);
        }

        return {
            statusCode: 200,
            body: JSON.stringify({ fileNames: outputKeys }),
        };
    } catch (error) {
        console.error("Error during translation process:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};
