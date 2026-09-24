package com.dormrepair.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
    String endpoint,
    String accessKey,
    String secretKey,
    String bucketName,
    Integer previewExpirySeconds,
    Boolean autoCreateBucket
) {
    public MinioProperties { if(bucketName==null||bucketName.isBlank())bucketName="repair-files";if(previewExpirySeconds==null)previewExpirySeconds=1800;if(autoCreateBucket==null)autoCreateBucket=true; }
}
