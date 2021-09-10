package gitlet;

import java.io.Serializable;

/**Blob class for Gitlet, for blob object.*/
public class Blob implements Serializable {

    /**The Things in the file.*/
    private byte[] contents;

    /**A BLOB object with CON.*/
    public Blob(byte[] con) {
        this.contents = con;
    }

    /**CONTENT of the file in a blob, and return thing.*/
    public byte[] getContents() {
        return this.contents;
    }
}
