const { S3Client, GetObjectCommand, PutObjectCommand } = require('@aws-sdk/client-s3');
const Jimp = require('jimp');
const stream = require('stream');

const s3 = new S3Client();

exports.handler = async (event) => {
    try {
        const bucket = event.Records[0].s3.bucket.name;
        const key = decodeURIComponent(event.Records[0].s3.object.key.replace(/\+/g, ' '));

        if (!key.startsWith('raw/')) {
            return;
        }

        const getCommand = new GetObjectCommand({ Bucket: bucket, Key: key });
        const response = await s3.send(getCommand);
        const imageBuffer = await streamToBuffer(response.Body);

        const image = await Jimp.read(imageBuffer);
        image.resize(800, Jimp.AUTO).quality(70); 
        const compressedBuffer = await image.getBufferAsync(Jimp.MIME_JPEG);

        const newKey = key.replace('raw/', '');

        const putCommand = new PutObjectCommand({
            Bucket: bucket,
            Key: newKey,
            Body: compressedBuffer,
            ContentType: 'image/jpeg'
        });
        await s3.send(putCommand);

        return { statusCode: 200, body: 'Success' };
    } catch (error) {
        console.error(error);
        throw error;
    }
};

const streamToBuffer = (stream) =>
    new Promise((resolve, reject) => {
        const chunks = [];
        stream.on('data', (chunk) => chunks.push(chunk));
        stream.on('error', reject);
        stream.on('end', () => resolve(Buffer.concat(chunks)));
    });
