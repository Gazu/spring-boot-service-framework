package com.smbtech.examples.openapicontract;

import com.smbtech.contracts.warehouseinventorycatalog.v1.api.DefaultApiController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(DefaultApiController.class)
@SpringBootApplication
public class OpenApiContractConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenApiContractConsumerApplication.class, args);
    }
}
