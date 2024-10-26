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
    try {
        const body = event.body ? JSON.parse(event.body) : event; // payload is different when triggering over APIGateway
        const fileNames = Array.isArray(body.fileNames) ? body.fileNames : [body.fileNames];
        const inputBucket = body.inputBucket;
        const inputLanguage = convertLanguageCode(body.inputLanguage) || "en-US";
        const outputKeys = [];

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
            statusCode: 200,
            body: JSON.stringify({
                fileNames: outputKeys,
            }),
        };
    } catch (error) {
        console.error("Error during transcription:", error);
        return {
            statusCode: error.statusCode || 500,
            body: error.message,
        };
    }
};

function convertLanguageCode(code) {
    const languageMap = {
        "ab": "ab-GE",
        "af": "af-ZA",
        "ar": "ar-SA", // default to Modern Standard Arabic
        "hy": "hy-AM",
        "ast": "ast-ES",
        "az": "az-AZ",
        "ba": "ba-RU",
        "eu": "eu-ES",
        "be": "be-BY",
        "bn": "bn-IN",
        "bs": "bs-BA",
        "bg": "bg-BG",
        "ca": "ca-ES",
        "ckb": "ckb-IQ", // default to Iraq
        "zh": "zh-CN", // default to Simplified Chinese
        "hr": "hr-HR",
        "cs": "cs-CZ",
        "da": "da-DK",
        "nl": "nl-NL",
        "en": "en-US", // default to US English
        "et": "et-ET",
        "fa": "fa-IR",
        "fi": "fi-FI",
        "fr": "fr-FR",
        "gl": "gl-ES",
        "ka": "ka-GE",
        "de": "de-DE",
        "el": "el-GR",
        "gu": "gu-IN",
        "ha": "ha-NG",
        "he": "he-IL",
        "hi": "hi-IN",
        "hu": "hu-HU",
        "is": "is-IS",
        "id": "id-ID",
        "it": "it-IT",
        "ja": "ja-JP",
        "kab": "kab-DZ",
        "kn": "kn-IN",
        "kk": "kk-KZ",
        "rw": "rw-RW",
        "ko": "ko-KR",
        "ky": "ky-KG",
        "lv": "lv-LV",
        "lt": "lt-LT",
        "lg": "lg-IN",
        "mk": "mk-MK",
        "ms": "ms-MY",
        "ml": "ml-IN",
        "mt": "mt-MT",
        "mr": "mr-IN",
        "mhr": "mhr-RU",
        "mn": "mn-MN",
        "nb": "nb-NO", // Norwegian Bokmål
        "or": "or-IN",
        "ps": "ps-AF",
        "pl": "pl-PL",
        "pt": "pt-PT", // default to Portugal Portuguese
        "pa": "pa-IN",
        "ro": "ro-RO",
        "ru": "ru-RU",
        "sr": "sr-RS",
        "si": "si-LK",
        "sk": "sk-SK",
        "sl": "sl-SI",
        "so": "so-SO",
        "es": "es-ES",
        "su": "su-ID",
        "sw": "sw-KE", // default to Kenya Swahili
        "sv": "sv-SE",
        "tl": "tl-PH",
        "ta": "ta-IN",
        "tt": "tt-RU",
        "te": "te-IN",
        "th": "th-TH",
        "tr": "tr-TR",
        "uk": "uk-UA",
        "ug": "ug-CN",
        "uz": "uz-UZ",
        "vi": "vi-VN",
        "cy": "cy-WL",
        "wo": "wo-SN",
        "zu": "zu-ZA"
    };

    // Check if the code is already in the full xx-XX format
    if (code.match(/^[a-z]{2,3}(-[A-Z]{2,3})?$/)) {
        return code;
    }

    // Otherwise, return the mapped code or undefined
    return languageMap[code] || undefined;
}
