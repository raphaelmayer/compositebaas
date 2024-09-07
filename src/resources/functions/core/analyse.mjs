import { S3Client, ListObjectsV2Command, HeadObjectCommand } from "@aws-sdk/client-s3";

const s3 = new S3Client();

export const handler = async (event) => {
    try {
        const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
        const inputBucket = body.inputBucket;
        const inputFileName = body.inputFileName;

        let fileNames = [];
        let fileSizes = [];

        if (inputFileName) {
            // If inputFileName is provided, get the specified file's size
            const s3Params = {
                Bucket: inputBucket,
                Key: inputFileName,
            };
            const command = new HeadObjectCommand(s3Params);
            const s3Object = await s3.send(command);
            fileNames.push(inputFileName);
            fileSizes.push(s3Object.ContentLength);
        } else {
            // Otherwise, list all objects in the inputBucket, handling pagination
            let isTruncated = true;
            let continuationToken = undefined;

            while (isTruncated) {
                const s3Params = {
                    Bucket: inputBucket,
                    ContinuationToken: continuationToken,
                };
                const command = new ListObjectsV2Command(s3Params);
                const s3Objects = await s3.send(command);

                // Add the fileNames to the result
                s3Objects.Contents.map((object) => {
                    fileNames.push(object.Key);
                    fileSizes.push(object.Size);
                });

                // Check if there are more fileNames to list
                isTruncated = s3Objects.IsTruncated;
                continuationToken = s3Objects.NextContinuationToken;
            }
        }

        const fileCount = fileNames.length;

        return {
            statusCode: 200,
            body: JSON.stringify({
                fileNames: fileNames,
                fileSizes: fileSizes,
                fileCount: fileCount,
            }),
        };
    } catch (error) {
        console.error("Error during analysis:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};
