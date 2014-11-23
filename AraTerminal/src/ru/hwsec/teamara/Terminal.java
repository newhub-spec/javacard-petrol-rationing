package ru.hwsec.teamara;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

// Question: Should we do APDU communication inside the individual functions or inside execute() ?

public abstract class Terminal {

    //Fields

    /* Trusted CA for verification of the CA's signature on card cert */
    private static byte[] cardIntermediateCA;

    /* cert 1 is used for mutual authentication and key exchange.
     * The signature is the hash of the concatenation of serialNumber + PublicKey + Expiry */

    private byte[] cert1SerialNumber;
    private byte[] cert1PubilcKey;
    private byte[] cert1PrivKey;
    private byte[] cert1Expiry;
    private byte[] cert1Signature;

	/* cert 2 is used for signatures for non-revocation. */
    private byte[] cert2SerialNumber;
    private byte[] cert2PubilcKey;
    private byte[] cert2PrivKey;
    private byte[] cert2Expiry;
    private byte[] cert2Signature;


    // APDU Communication
    static final byte[] ARA_APPLET_AID = { (byte) 0xde, (byte) 0xad, (byte) 0xba, (byte) 0xbe, (byte) 0x01 };
    static final CommandAPDU SELECT_APDU = new CommandAPDU((byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x00, ARA_APPLET_AID);

    CardChannel applet;



    // Methods

    public Terminal(){ };

    /* verify that the certificate of card is correctly signed by card CA
     * and that the certificate is not expired */
    abstract boolean verifyCert(byte[] cardSerialNumber, byte[] cardPublicKey, byte[] cardExpiry, byte[] cardSignature);

    /* Perform mutual authentication with the card.
     * Establish shared Diffie-Hellman Secret from Public Keys
     * Return secret key from DH Secret and nonces (used later in encrypt/decrypt)
     *  - client write MAC key
     *  - server write MAC key
     *  - client write encryption key
     *  - server write encryption key
     *  - client write IV
     *  - server write IV
     */
    abstract byte[] mutualAuth(int nonceCard, int nonceTerminal, byte[] cardPublicKey);

    /* Makes digital signature on transaction for non-repudiation */
    abstract byte[] signTransaction(byte[] transaction);

    /* Decrypt a received APDU payload.
    * Ciphertext Block comprises:
    *   - 16 byte IV
    *   - Ciphertext AES_CBC(msg + padding)
    *   - Integrity check (MAC(MAC_write_key, seq_num + plaintext))
    *   Ref: RFC 5246 6.2.3.2 (CBC) and 6.2.3.1 (HMAC)
    * Returns plaintext */
    abstract byte[] decrypt(byte[] ciphertext, byte[] cardEncKey, byte[] cardMACKey);


    /* Encrypt an APDU payload for sending.
     * Output ciphertext */
    abstract byte[] encrypt(byte[] plaintext, byte[] terminalEncKey, byte[] terminalMACKey);


    /* Ask user for a pin.
     * Send pin to card
     * return true if card returns success, else return false */
    abstract boolean pinCheck();


    /* Check the revocation status of the card.
     * Return false if revoked.
     * Do not proceed further and end all communications with the card.*/
    abstract boolean checkRevoke();


    /* Main state machine that calls the various methods defined above? */
    private void execute() {
    	TerminalFactory tf = TerminalFactory.getDefault();
    	CardTerminals ct = tf.terminals();
    	List<CardTerminal> cs;
		try {
			cs = ct.list(CardTerminals.State.CARD_PRESENT);
			if (cs.isEmpty()) {
	    		System.out.println("No terminals with a card found.");
	    		return;
	    	}
			while (true) {
	    		for(CardTerminal c : cs) {
	    			if (c.isCardPresent()) {
	    				Card card = c.connect("*");
	    	    		this.applet = card.getBasicChannel();
	    	    		ResponseAPDU resp = this.applet.transmit(SELECT_APDU);
	    	    		if (resp.getSW() != 0x9000) {
	    	    			throw new CardException("Could no select AraApplet.");
	    	    		}
	    	    		resp = this.applet.transmit(new CommandAPDU(0, 0x0a, 0, 0));
	    	    		byte[] data = resp.getData();
	    	    		int x;
	    	    		x = 2;


	    	    	}
	    		}
	    	}


		} catch (CardException e) { }
    }

}