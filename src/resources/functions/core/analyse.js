import { S3Client, ListObjectsV2Command, HeadObjectCommand } from "@aws-sdk/client-s3";

const s3 = new S3Client();

export const handler = async (event) => {
    const inputBucket = event.inputBucket;
    const inputFileName = event.inputFileName;

    try {
        let files = [];

        if (inputFileName) {
            // If inputFileName is provided, get the specified file's size
            const s3Params = {
                Bucket: inputBucket,
                Key: inputFileName,
            };
            const command = new HeadObjectCommand(s3Params);
            const s3Object = await s3.send(command);
            files.push({
                key: "s3://" + inputBucket + "/" + inputFileName,
                size: s3Object.ContentLength,
            });
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

                // Add the files to the result
                files = files.concat(
                    s3Objects.Contents.map((object) => ({
                        key: "s3://" + inputBucket + "/" + object.Key,
                        size: object.Size,
                    }))
                );

                // Check if there are more files to list
                isTruncated = s3Objects.IsTruncated;
                continuationToken = s3Objects.NextContinuationToken;
            }
        }

        const fileCount = files.length;

        // Return the output in the required format
        return {
            fileNames: files,
            fileCount: fileCount,
        };
    } catch (error) {
        console.error("Error during analysis:", error);
        throw new Error("AnalysisFailed");
    }
};
