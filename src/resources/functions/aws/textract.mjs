import { S3Client, GetObjectCommand, PutObjectCommand } from "@aws-sdk/client-s3";
import { TextractClient, DetectDocumentTextCommand } from "@aws-sdk/client-textract";
import { Buffer } from "buffer";

const s3Client = new S3Client({});
const textractClient = new TextractClient({});

const streamToBuffer = async (stream) => {
    return new Promise((resolve, reject) => {
        const chunks = [];
        stream.on("data", (chunk) => chunks.push(chunk));
        stream.on("end", () => resolve(Buffer.concat(chunks)));
        stream.on("error", reject);
    });
};

async function uploadToS3(bucket, key, body, contentType) {
    const putObjectParams = {
        Bucket: bucket,
        Key: key,
        Body: body,
        ContentType: contentType,
    };
    await s3Client.send(new PutObjectCommand(putObjectParams));
}

export const handler = async (event) => {
    try {
        const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
        const fileNames = body.fileNames;
        const inputBucket = body.inputBucket;
        const outputBucket = body.outputBucket || inputBucket;
        const outputKeys = [];

        for (const fileName of fileNames) {
            const getObjectCommand = new GetObjectCommand({
                Bucket: inputBucket,
                Key: fileName,
            });

            const data = await s3Client.send(getObjectCommand);
            const fileBuffer = await streamToBuffer(data.Body);

            // Extract text from the document using Textract
            const textractCommand = new DetectDocumentTextCommand({
                Document: {
                    Bytes: fileBuffer,
                },
            });

            const textractResponse = await textractClient.send(textractCommand);

            // Extract text lines from Textract response
            const extractedText = textractResponse.Blocks.filter((block) => block.BlockType === "LINE")
                .map((block) => block.Text)
                .join("\n");

            if (!extractedText) {
                throw new Error(`No text detected in document: ${fileName}`);
            }

            // Upload the extracted text to S3
            const outputKey = `${fileName.replace(/\.[^/.]+$/, "")}-extracted-${Date.now()}.txt`; // Generate output file name
            await uploadToS3(outputBucket, outputKey, extractedText, "text/plain");
            console.log(`Extracted text uploaded to S3: ${outputKey}`);

            outputKeys.push(outputKey);
        }

        return {
            statusCode: 200,
            body: JSON.stringify({ fileNames: outputKeys }),
        };
    } catch (error) {
        console.error("Error processing the request:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};
