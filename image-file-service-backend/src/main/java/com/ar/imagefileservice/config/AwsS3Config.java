package com.ar.imagefileservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AwsS3Config {
    
    @Value("${aws.accessKeyId}")
    private String accessKeyId;
    
    @Value("${aws.secretKey}")
    private String secretKey;
    
    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.s3.bucketName}")
    private String bucketName;
    
    @Bean
    public AmazonS3 amazonS3Client() {
        System.out.println("AWS Region being used: " + region); // Debug line
        
        BasicAWSCredentials creds = new BasicAWSCredentials(accessKeyId, secretKey);
        return AmazonS3ClientBuilder.standard()
            .withCredentials(new AWSStaticCredentialsProvider(creds))
            .withRegion("eu-north-1")
            .build();
    }
}