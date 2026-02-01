package com.restaurantback.utils;

import com.amazonaws.services.s3.model.PutObjectResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.util.Base64;
import java.util.UUID;
import java.util.Date;

public class Base64ImageHandler {

    private final String S3_BUCKET_NAME;
    private final AmazonS3 s3Client;
    private final Logger logger = LoggerFactory.getLogger(Base64ImageHandler.class);

    public Base64ImageHandler(AmazonS3 s3Client) {
        this.s3Client = s3Client;
        this.S3_BUCKET_NAME = System.getenv("profileImageBucket");
    }

    public String handleBase64Image(String base64Image) {
        String fileName = uploadBase64ImageToS3(base64Image);
        return  generatePresignedUrl(fileName);
    }

    private String uploadBase64ImageToS3(String base64Image) {
        try {
            logger.info("upload base 64 image to s3...");

            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            logger.info("image in bytes: {}", imageBytes);

            String fileName = UUID.randomUUID().toString() + ".jpg"; // Assuming the image is a JPEG
            logger.info("image file name: {}", fileName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("image/jpeg"); // Adjust based on the image type
            metadata.setContentLength(imageBytes.length);
            logger.info("put object request: {}", metadata);

            // Upload the image to S3
            PutObjectResult result = s3Client.putObject(S3_BUCKET_NAME, fileName, new java.io.ByteArrayInputStream(imageBytes), metadata);

            logger.info("put object response: {}", result);
            return fileName;

        } catch (Exception e) {
            logger.error(e.toString());
            throw  new RuntimeException(e.getMessage());
        }
    }

    private String generatePresignedUrl(String fileName) {
        // Create an S3Presigner instance
        try  {
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 60 * 12; // Add 12 hour
            expiration.setTime(expTimeMillis);

            // Generate the pre-signed URL
            GeneratePresignedUrlRequest generatePresignedUrlRequest =
                    new GeneratePresignedUrlRequest(S3_BUCKET_NAME, fileName)
                            .withMethod(com.amazonaws.HttpMethod.GET)
                            .withExpiration(expiration);

            return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
        } catch (Exception e) {
            logger.error(e.toString());
            throw new RuntimeException(e.getMessage());
        }
    }

}