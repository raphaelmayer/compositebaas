import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";
import ffmpeg from "fluent-ffmpeg";
import fs from "fs";
import { tmpdir } from "os";
import { join } from "path";

const s3Client = new S3Client();

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

// Merge multiple media files into one
async function mergeMediaFiles(inputFiles, outputFilePath) {
    return new Promise((resolve, reject) => {
        const ffmpegCommand = ffmpeg();

        inputFiles.forEach(file => ffmpegCommand.input(file));

        ffmpegCommand
            .on("end", () => resolve(outputFilePath))
            .on("error", reject)
            .mergeToFile(outputFilePath, tmpdir());
    });
}

// Main handler function for merging audio/video
export const handler = async (event) => {
    const body = event.body ? JSON.parse(event.body) : event;
    const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
    const inputBucket = body.inputBucket;
    const outputBucket = body.outputBucket || inputBucket;
    const outputFileName = `${fileNames[0].split("-")[0]}-merged.mp4`; // Change extension based on media type

    const tempDir = tmpdir();
    const inputFiles = [];
    try {
        // Load each media file from S3
        for (const fileName of fileNames) {
            const inputFilePath = await loadFromS3(inputBucket, fileName, tempDir);
            console.log(`Loaded file from S3: ${fileName}`);
            inputFiles.push(inputFilePath);
        }

        // Merge media files
        const mergedFilePath = join(tempDir, outputFileName);
        await mergeMediaFiles(inputFiles, mergedFilePath);
        console.log(`Merged files into: ${mergedFilePath}`);

        // Save merged file back to S3
        await saveInS3(mergedFilePath, outputBucket, outputFileName);
        console.log(`Saved merged file to S3: ${outputFileName}`);

        return {
            fileNames: [outputFileName],
        };
    } catch (error) {
        console.error("Error during merging process:", error);
        return { error: error.message };
    }
};
