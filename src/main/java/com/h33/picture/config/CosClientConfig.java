package com.h33.picture.config;

import cn.hutool.core.lang.copier.SrcToDestCopier;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.qcloud.cos.COSClient;



@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {
    //域名
    private String host;

    private String secretId;
    private String secretKey;

    //区域
    private String region;

    //桶名
    private String bucket;

    @Bean
    public COSClient cosClient(){
//        初始化用户信息 id+key
        COSCredentials cred=new BasicCOSCredentials(secretId,secretKey);
        //设置桶区域
        ClientConfig clientConfig=new ClientConfig(new Region(region));
        return new COSClient(cred,clientConfig);


    }

}
