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

function normalizeFileNames(fileNames) {
    // Map over each group
    const normalized = fileNames.map(group => 
      group.map(item => Array.isArray(item) && item.length === 1 ? item[0] : item)
    );
  
    // Flatten if each group has only one item
    return normalized.every(group => group.length === 1)
      ? normalized.flat()
      : normalized;
  }

// Merge multiple text files from S3 where fileNames is an array of arrays
export const handler = async (event) => {
    const body = event.body ? JSON.parse(event.body) : event; // Payload structure depends on API Gateway or direct trigger
    const fileGroups = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames]; // Each group is an array of file parts
    const inputBucket = body.inputBucket;
    const outputBucket = body.outputBucket || inputBucket;
    const mergedFiles = [];
    const fileParts = normalizeFileNames(fileGroups);
    console.log(normalizeFileNames(fileGroups))

    try {
        // Process each group of file parts
        // for (const fileParts of normalizeFileNames(fileGroups)) {
            if (!Array.isArray(fileParts)) {
                throw new Error("fileNames should be an array of arrays.");
            }

            const outputFileName = `${fileParts[0].split("-")[0]}-merged.txt`;
            let mergedText = "";

            // Load and merge each part of the file
            for (const fileName of fileParts) {
                const fileContent = await loadFromS3(inputBucket, fileName);
                console.log(`Loaded file part from S3: ${fileName}`);
                mergedText += fileContent + "\n"; // Add a newline between parts
            }

            // Save the merged file to S3
            await saveInS3(mergedText, outputBucket, outputFileName);
            console.log(`Merged file saved to S3: ${outputFileName}`);
            mergedFiles.push(outputFileName);
        // }

        return {
            statusCode: 200,
            body: JSON.stringify({
                fileNames: mergedFiles,
            }),
        };
    } catch (error) {
        console.error("Error during merge process:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};
