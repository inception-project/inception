package de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageServiceImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;

@Configuration
@EnableConfigurationProperties({ CasStoragePropertiesImpl.class, BackupProperties.class })
public class CasStorageServiceAutoConfiguration
{
    @Bean(CasStorageService.SERVICE_NAME)
    public CasStorageService casStorageService(@Autowired(required = false) CasDoctor aCasDoctor,
            @Autowired(required = false) AnnotationSchemaService aSchemaService,
            RepositoryProperties aRepositoryProperties, CasStorageProperties aCasStorageProperties,
            BackupProperties aBackupProperties)
    {
        return new CasStorageServiceImpl(aCasDoctor, aSchemaService, aRepositoryProperties,
                aCasStorageProperties, aBackupProperties);
    }
}
