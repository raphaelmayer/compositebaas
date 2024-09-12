import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";
import ffmpeg from "fluent-ffmpeg";
import fs from "fs";
import { tmpdir } from "os";
import { join } from "path";

const s3Client = new S3Client();
ffmpeg.setFfmpegPath("/opt/nodejs/ffmpeg");

async function loadFromS3(bucket, key, filePath) {
    const params = {
        Bucket: bucket,
        Key: key,
    };
    const command = new GetObjectCommand(params);
    const tempFilePath = join(filePath, key.split("/").pop());

    try {
        const response = await s3Client.send(command);
        const stream = response.Body.pipe(fs.createWriteStream(tempFilePath));

        return new Promise((resolve, reject) => {
            stream.on("finish", () => resolve(tempFilePath));
            stream.on("error", reject);
        });
    } catch (err) {
        throw new Error(`Error loading from S3: ${err.message}`);
    }
}

async function saveInS3(filePath, bucket, key) {
    const fileStream = fs.createReadStream(filePath);
    const params = {
        Body: fileStream,
        Bucket: bucket,
        Key: key,
    };
    await s3Client.send(new PutObjectCommand(params));
    console.log(`Saved file "${key}" in bucket "${bucket}"`);
}

// Split audio/video into parts
async function splitMediaFile(inputFile, outputPath, numParts) {
    const duration = await new Promise((resolve, reject) => {
        ffmpeg.ffprobe(inputFile, (err, metadata) => {
            if (err) return reject(err);
            resolve(metadata.format.duration);
        });
    });

    const partDuration = duration / numParts;
    const outputFiles = [];

    for (let i = 0; i < numParts; i++) {
        const outputFile = `${outputPath}-part-${i + 1}.mp4`; // Can change extension based on media type
        outputFiles.push(outputFile);

        await new Promise((resolve, reject) => {
            ffmpeg(inputFile)
                .setStartTime(partDuration * i)
                .setDuration(partDuration)
                .output(outputFile)
                .on("end", resolve)
                .on("error", reject)
                .run();
        });
    }

    return outputFiles;
}

// Main handler function for splitting audio/video
export const handler = async (event) => {
    const body = event.body ? JSON.parse(event.body) : event;
    const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
    const inputBucket = body.inputBucket;
    const outputBucket = body.outputBucket || inputBucket;
    const numParts = body.numParts || 3; // Default to 2 parts if not specified
    const outputKeys = [];

    const tempDir = tmpdir();
    try {
        for (const fileName of fileNames) {
            // Load media file from S3
            const inputFilePath = await loadFromS3(inputBucket, fileName, tempDir);
            console.log(`Loaded file from S3: ${fileName}`);

            // Split media file
            const splitFiles = await splitMediaFile(inputFilePath, join(tempDir, fileName.split(".")[0]), numParts);
            console.log(`File split into ${numParts} parts`);

            // Save split parts back to S3
            for (const splitFile of splitFiles) {
                const outputKey = `${fileName.split(".")[0]}-${splitFile.split("/").pop()}`;
                await saveInS3(splitFile, outputBucket, outputKey);
                console.log(`Saved split part to S3: ${outputKey}`);
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
