import { S3Client, GetObjectCommand, PutObjectCommand, DeleteObjectCommand } from "@aws-sdk/client-s3";
import { TranscribeClient, StartTranscriptionJobCommand, GetTranscriptionJobCommand } from "@aws-sdk/client-transcribe";

const transcribe = new TranscribeClient();
const s3 = new S3Client();

// Helper function to wait for a certain time
const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function streamToString(stream) {
    const chunks = [];
    for await (const chunk of stream) {
        chunks.push(chunk);
    }
    return Buffer.concat(chunks).toString("utf-8");
}

async function getTranscriptionJson(bucket, key) {
    const getObjectParams = {
        Bucket: bucket,
        Key: key,
    };
    const result = await s3.send(new GetObjectCommand(getObjectParams));
    const jsonContent = await streamToString(result.Body);
    return JSON.parse(jsonContent);
}

async function saveTranscriptionText(bucket, key, transcript) {
    const putObjectParams = {
        Bucket: bucket,
        Key: key,
        Body: transcript,
        ContentType: "text/plain",
    };
    await s3.send(new PutObjectCommand(putObjectParams));
}

async function deleteTranscriptionFile(bucket, key) {
    const deleteObjectParams = {
        Bucket: bucket,
        Key: key,
    };
    await s3.send(new DeleteObjectCommand(deleteObjectParams));
}

// Helper function to poll for transcription job completion
async function pollTranscriptionJob(transcribe, jobId) {
    let jobStatus = "IN_PROGRESS";
    let transcriptionUri = null;

    while (jobStatus === "IN_PROGRESS") {
        console.log("Waiting 10 seconds before polling the job status...");
        await delay(10000); // Poll every 10 seconds

        const getJobCommand = new GetTranscriptionJobCommand({ TranscriptionJobName: jobId });
        const jobStatusResponse = await transcribe.send(getJobCommand);

        jobStatus = jobStatusResponse.TranscriptionJob.TranscriptionJobStatus;
        console.log(`Current job status: ${jobStatus}`);

        if (jobStatus === "COMPLETED") {
            transcriptionUri = jobStatusResponse.TranscriptionJob.Transcript.TranscriptFileUri;
            console.log(`Job completed. Transcript URI: ${transcriptionUri}`);
            return transcriptionUri;
        } else if (jobStatus === "FAILED") {
            const failureReason = jobStatusResponse.TranscriptionJob.FailureReason;
            console.error(`Job failed: ${failureReason}`);
            throw new Error(`Transcription job failed: ${failureReason}`);
        }
    }
}

export const handler = async (event) => {
    const fileNames = event.fileNames;  
    const inputBucket = event.inputBucket;
    const inputLanguage = event.inputLanguage || "en-US"; 
    const outputKeys = []; 

    try {
        for (const fileName of fileNames) {
            // Submit the transcription job
            const jobName = `${fileName.replace(/\.[^/.]+$/, "")}-transcribed-${Date.now()}.txt`;
            const transcriptionParams = {
                TranscriptionJobName: jobName,
                LanguageCode: inputLanguage,
                Media: {
                    MediaFileUri: `s3://${inputBucket}/${fileName}`,
                },
                OutputBucketName: inputBucket,
            };

            const startJobCommand = new StartTranscriptionJobCommand(transcriptionParams);
            const jobResult = await transcribe.send(startJobCommand);
            const jobId = jobResult.TranscriptionJob.TranscriptionJobName;
            console.log(`Transcription job submitted: ${jobId}`);

            // Poll the transcription job status
            const transcriptionUri = await pollTranscriptionJob(transcribe, jobId);

            // Get the transcription JSON from S3
            const transcriptionFileKey = transcriptionUri.split("/").pop(); // Extract the key from the URI
            const transcriptionData = await getTranscriptionJson(inputBucket, transcriptionFileKey);
            // Extract the actual transcript text from the JSON metadata
            const transcriptText = transcriptionData.results.transcripts.map((t) => t.transcript).join("\n");

            // Save the transcript as a text file in the same S3 bucket
            const transcriptKey = fileName.replace(/\.\w+$/, ".txt"); // Replace original extension with .txt
            await saveTranscriptionText(inputBucket, transcriptKey, transcriptText);
            console.log(`Transcript saved as text file: ${transcriptKey}`);

            // Delete the original JSON transcription file
            await deleteTranscriptionFile(inputBucket, transcriptionFileKey);
            console.log(`Original transcription file deleted: ${transcriptionFileKey}`);

            outputKeys.push(transcriptKey);
        }

        return {
            fileNames: outputKeys,
        };

    } catch (error) {
        console.error("Error during transcription:", error);
        return { error: error.message };
    }
};
