package security.pgp.bc;

import configuration.SecurityTestConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;
import org.datadiode.black.service.PGPService;
import org.datadiode.black.util.*;
import org.datadiode.black.util.PGPUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.util.Iterator;

/**
 * Created by marcelmaatkamp on 11/11/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SecurityTestConfiguration.class)
@EnableAutoConfiguration
public class PGPTests {
    private static final Logger log = LoggerFactory.getLogger(PGPTests.class);


    String pubringFilename = "security/pgp/pubring.gpg";
    String secringFilename = "security/pgp/secring.gpg";

    @Autowired
    ApplicationContext applicationContext;


    @Autowired
    PGPService pgpService;

    @Test
    public void testKeys() throws IOException, PGPException, NoSuchProviderException {
        Resource secringResource = applicationContext.getResource(secringFilename);
        Resource pubringResource = applicationContext.getResource(pubringFilename);

        PGPPrivateKey pgpPrivateKey = PGPUtil.findPrivateKey(new FileInputStream(secringResource.getFile()), -2742202535458887244L, null);

        PGPPublicKey pgpPublicKey = PGPUtil.readPublicKey(new FileInputStream(pubringResource.getFile()));
        log.info("key: " + pgpPublicKey);
    }
}
