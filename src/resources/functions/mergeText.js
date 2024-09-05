import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";

const s3Client = new S3Client();

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

async function saveInS3(data, bucket, key) {
    const params = {
        Body: data,
        Bucket: bucket,
        Key: key,
    };
    await s3Client.send(new PutObjectCommand(params));
    console.log(`Saved file "${key}" in bucket "${bucket}"`);
}

// Merge multiple text files from S3
export const handler = async (event) => {
    const inputBucket = event.inputBucket;
    const outputBucket = event.outputBucket || inputBucket;
    const fileNames = event.fileNames; // Array of file names to be merged
    const outputFileName = `${fileNames[0].split("-")[0]}-merged.txt`;
    let mergedText = "";

    try {
        // Load and merge each file's content
        for (const fileName of fileNames) {
            const fileContent = await loadFromS3(inputBucket, fileName);
            console.log(`Loaded file from S3: ${fileName}`);
            mergedText += fileContent + "\n"; // Add a newline between files
        }

        // Save the merged text to S3
        await saveInS3(mergedText, outputBucket, outputFileName);
        console.log(`Merged file saved to S3: ${outputFileName}`);

        return {
            fileNames: [outputFileName],
        };
        
    } catch (error) {
        console.error("Error during merge process:", error);
        return { error: error.message };
    }
};
