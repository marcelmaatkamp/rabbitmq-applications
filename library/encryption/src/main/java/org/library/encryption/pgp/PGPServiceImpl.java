package org.datadiode.black.service;

import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRingCollection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created by marcelmaatkamp on 11/11/15.
 */
public class PGPServiceImpl implements PGPService {

    @SuppressWarnings("unchecked")
    public static PGPPublicKey readPublicKey(InputStream in) throws IOException, PGPException {

        in = PGPUtil.getDecoderStream(in);

        // Use BC public key ring collection for backwards compatibility
        PGPPublicKeyRingCollection pgpPub = new BcPGPPublicKeyRingCollection(in);

        // Loop through the collection until we find a key suitable for encryption
        // (in the real world you would probably want to be a bit smarter about this)
        PGPPublicKey key = null;

        // Iterate through the key rings
        Iterator<PGPPublicKeyRing> rIt = pgpPub.getKeyRings();

        while (key == null && rIt.hasNext()) {

            PGPPublicKeyRing kRing = rIt.next();
            Iterator<PGPPublicKey> kIt = kRing.getPublicKeys();
            while (key == null && kIt.hasNext()) {
                PGPPublicKey k = kIt.next();

                if (k.isEncryptionKey()) {
                    key = k;
                }
            }

        }

        if (key == null) {
            throw new IllegalArgumentException("Can't find encryption key in key ring.");
        }

        return key;
    }
}

