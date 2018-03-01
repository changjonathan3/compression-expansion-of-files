/**
 * @author Jonathan Chang
 * Assig2 MyLZW
 * CS1501 Spring18
 * Garrison
 */

public class MyLZW {
    private static final int MAXW = 16; //max bit size WIDTH for codeword given, saved to know we are full
    private static final int MINW = 9; //min bit size WIDTH for codeword given, saved for reset
    private static final int MAXL = 65536;//max NUM codewords 2^16, save to know we are full
    private static final int MINL = 512;//min NUM codewords 2^9, saved for reset
    private static final int R = 256;    // number of input chars

    private static int W = 9;   //initial WIDTH 9 will change during run
    private static int L = 512;  // initial NUM of codewords = 2^W, will change during run

    private static char option = 'n'; //default NOTHING mode
    //added for MONITOR mode according to assignment
    private static double uncompressed =0; // uncompressed/compressed gives compression ratio
    private static double compressed =0;
    private static double newRatio =0; // used in monitor function
    private static final double RESETCR = 1.1; //strict threshold compression ratio given

    private static final int CHAR_SZ = 8;//bit size for char

    //compress variables
    private static boolean compressing = false; //this is used to know which function to reset
    public static int code = 0; //this is used to track codewords in compress
    public static TST<Integer> st;//this is used as dictionary in compress, and to reset

    //expand variables
    public static int index = 0;//this is used to track codewords in expand
    public static String[] at;//this is used as dictionary in expand, and to reset

    // Do not instantiate.
    private MyLZW() { }

    /**
     * Reads a sequence of 8-bit bytes from standard input; compresses
     * them using LZW compression with variable bit codewords; and writes the results
     * to standard output.
     */

    public static void compress() {
        newRatio=0;//first run through of compress
        compressing=true; //properly reset when needed
        String input = BinaryStdIn.readString();
        st = new TST<>();
        for (int i = 0; i < R; i++)
            st.put("" + (char) i, i);
        code = R+1;  // R is codeword for EOF. Next new codeword would be 256
        //codebook initialized to all single characters
        W=MINW; //start from lowest bit width
        L=MINL; //lowest num codewords
        while (input.length() > 0) {
            String s = st.longestPrefixOf(input);  // Find max prefix match s in codebook
            if(st.get(s)<MAXL){ //prevents error in 'n' mode from overflowing
                BinaryStdOut.write(st.get(s), W);      // Print s's encoding, our codeword
                compressed+=W; //size of compressed data generated
            }
            int t = s.length();
            uncompressed+=(t * CHAR_SZ);//all uncompressed data processed, input bit size

            if(t<input.length()){ //multiple cases for successful add to dictionary
                if(code<L) // Add s to symbol table dictionary.
                    st.put(input.substring(0, t + 1), code++); // longest prefix + next char in file
                    //add result to dictionary with new codeword
                else{ // need to adjust sizes
                    if(W<MAXW){ //still room left in codebook to increase
                        W++; //increase bits per codeword value
                        L*=2; //increase num of codewords
                        st.put(input.substring(0, t + 1), code++);
                    }
                    else{ //full codebook, mode options
                        if(option == 'r'){ //reset
                            resetCompress();
                        }
                        if(option == 'm'){//reset if needed
                            monitor(uncompressed, compressed, newRatio);
                        }
                        st.put(input.substring(0, t + 1), code++);
                    }
                } //end else
            } //end if
            input = input.substring(t);            // Scan past s in input.
        } //end while
        compressing=false;//will expand next so reset this
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
    }//end compress

    private static void resetCompress(){
        //same as start as compress() function
        st = new TST<>();
        for (int i = 0; i < R; i++)
            st.put("" + (char) i, i);
        code = R+1;  // R is codeword for EOF
        W=MINW;
        L=MINL;
    }

    private static void monitor
    (double uncompressed, double compressed, double newRatio){//used in compress and expand
        if(newRatio == 0){//first run, no old ratio
            newRatio = uncompressed/compressed;
        }
        else {
            double oldRatio = newRatio;
            newRatio = uncompressed/compressed;
            if(oldRatio/newRatio > RESETCR){//take ratio of ratios
                if(compressing)
                    resetCompress();
                else
                    resetExpand();
            }
        }//end else
    }//end monitor

    /**
     * Reads a sequence of bit encoded using LZW compression with
     * variable bit codewords from standard input; expands them; and writes
     * the results to standard output.
     */

    public static void expand() { //ADD uncompress and compress calculations
        newRatio=0;//reset for first run through of expand
        at = new String[MAXL]; //to hold max amount of codewords
        // initialize symbol table codebook with all 1-character strings
        for (index = 0; index < R; index++)
            at[index] = "" + (char) index;
        at[index++] = "";                        // (unused) lookahead for EOF

        W=MINW; //start from lowest bit width
        L=MINL; //start from lowest num codewords
        int codeword = BinaryStdIn.readInt(W);//read next codeword from file
        if (codeword == R){
            return;           // expanded message is empty string
        }
        String val = at[codeword];          //lookup corresponding pattern in codebook
        while (true) { //use i to track codes
            if(index==L){ //possible size increase
                if(W<MAXW){ //not full, increase sizes
                    W++;
                    L*=2;
                }
                else{ //full
                    if(option == 'r'){ //reset
                        resetExpand();
                    }
                    if(option == 'm'){//reset if needed
                        monitor(uncompressed, compressed, newRatio);
                    }
                }
            }//end if
            BinaryStdOut.write(val);//output codebook pattern
            uncompressed+=(val.length()*CHAR_SZ);//amount of uncompressed data generated
            codeword = BinaryStdIn.readInt(W); //read next codeword
            compressed+=W;//amount of compressed data processed
            if (codeword == R){
                break;
            }
            String s = at[codeword]; //lookup corresponding pattern in codebook
            if (index == codeword) s = val + val.charAt(0);   // special case hack
            if (index < L) {
                at[index++] = val + s.charAt(0); // add prev pattern + first char curr to codebook
            }
            val = s;
        }//end while
        BinaryStdOut.close();
    }//end expand

    private static void resetExpand(){//same as start of expand function
        at = new String[MAXL]; //to hold max amount of codewords

        // initialize symbol table codebook with all 1-character strings
        for (index = 0; index < R; index++)
            at[index] = "" + (char) index;
        at[index++] = "";                        // (unused) lookahead for EOF
        W=MINW;
        L=MINL;
    }

    /**
     * Sample client that calls {@code compress()} if the command-line
     * argument is "-" an {@code expand()} if it is "+".
     *
     * @param args the command-line arguments
     */

    public static void main(String[] args) {
        if (args[0].equals("-")) {
            option = args[1].charAt(0); //mode option selected. this then will apply to expand function
            BinaryStdOut.write(option, CHAR_SZ);//save the option as first char in file so we can access in expand()
            compress();
        }
        else if (args[0].equals("+")){
            //get the option we used in compress() by reading in first char available in the lzw file
            option = BinaryStdIn.readChar(CHAR_SZ);
            expand(); //directly expand. mode option will be taken from lzw file
        }
        else throw new IllegalArgumentException("Illegal command line argument");
    }
}
