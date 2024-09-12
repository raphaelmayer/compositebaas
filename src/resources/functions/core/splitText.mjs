import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";

const s3Client = new S3Client();

// Save data to S3
async function saveInS3(data, bucket, key) {
    const params = {
        Body: data,
        Bucket: bucket,
        Key: key,
    };
    await s3Client.send(new PutObjectCommand(params));
    console.log(`Saved file "${key}" in bucket "${bucket}"`);
}

// Load data from S3
async function loadFromS3(bucket, key) {
    const params = {
        Bucket: bucket,
        Key: key,
    };
    const command = new GetObjectCommand(params);

    try {
        const response = await s3Client.send(command);
        let chunks = [];
        for await (let chunk of response.Body) {
            chunks.push(chunk);
        }
        return Buffer.concat(chunks).toString("utf-8");
    } catch (err) {
        throw new Error(`Error loading from S3: ${err.message}`);
    }
}

// Parse the file name to remove extension
function parseFileName(fileName) {
    const lastDotIndex = fileName.lastIndexOf(".");
    return fileName.substring(0, lastDotIndex);
}

// Split text into n parts
function splitText(text, numParts) {
    const partSize = Math.ceil(text.length / numParts);
    const parts = [];
    for (let i = 0; i < text.length; i += partSize) {
        parts.push(text.slice(i, i + partSize));
    }
    return parts;
}

// Main handler function
export const handler = async (event) => {
    const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
    const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
    const inputBucket = body.inputBucket;
    const outputBucket = body.outputBucket || inputBucket;
    const numParts = body.numParts || 2; // Default to 2 parts if not specified
    const outputKeys = [];

    try {
        for (const fileName of fileNames) {
            const inputText = await loadFromS3(inputBucket, fileName);
            console.log(`Loaded file from S3: ${fileName}`);

            // Split file into specified number of parts
            const parts = splitText(inputText, numParts);
            console.log(`File: ${fileName} split into ${numParts} parts`);

            // Save each part to S3
            for (const [i, part] of parts.entries()) {
                const outputKey = `${parseFileName(fileName)}-part-${i + 1}.txt`;
                await saveInS3(part, outputBucket, outputKey);
                console.log(`Part saved to S3: ${outputKey}`);
                outputKeys.push(outputKey);
            }
        }

        return {
            statusCode: 200,
            body: JSON.stringify({
                fileNames: outputKeys,
            }),
        };
    } catch (error) {
        console.error("Error during splitting process:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};
