/* Linker Version 1 - Dylan Sheppard
 * Spring 2015
 */
import java.io.*;
class Endian {
   short reverseOrder(short x)
   {
      int y;
      if (System.getProperty("os.arch").equals ("x86")) {
         y = ((int) x) & 0xffff;
         return (short) (256 * (y%256) + y/256); 
      }
      else
         return x;
   }
}

class PE extends Endian {
   short address[];
   String symbol[];
   int index;
   int maxSize;
   public PE(int mSize)
   {
      maxSize = mSize;
      address = new short[mSize];
      symbol = new String[mSize];
      index = 0;
   }
   public int search(String s)
   {
      int i = 0;
      while (i < index && !symbol[i].equals(s))
         i++;
      if (i == index)
         return -1;
      else
         return i;
   }
   public short getadd(int i) {
      return address[i];
   }
   public String getsym(int i) {
      return symbol[i];
   }
   public int size() {
      return index;
   }
}

class P extends PE {
   public P(int size) {
      super(size);
   }
   public void add(short a, short ma, String s)
   {
      for (int i = 0; i < index; i++) 
         if (s.equals(symbol[i])) {
            System.out.println("ERROR: Duplicate PUBLIC symbol "
            + s);
            System.exit(1);
         }
      if (index >= maxSize) {
         System.out.println("ERROR: P table overflow");
         System.exit(1);
      }
      address[index] = (short)(a + ma);  
      symbol[index++] = s;
   }
   public void write(DataOutputStream s) throws IOException {
      for (int i = 0; i < index; i++) {
           s.writeByte('P');
           s.writeShort(reverseOrder(address[i]));
           s.writeBytes(symbol[i]);
      }
   }
}

class E extends PE {
   public E(int size) {
      super(size);
   }
   public void add(short a, short ma, String s)
   {
      if (index >= maxSize) {
         System.out.println("ERROR: E table overflow");
         System.exit(1);
      }
      address[index] = (short)(a + ma); 
      symbol[index++] = s;
   }
   public void write(DataOutputStream s) throws IOException{
//MISSING CODE
	   for (int i = 0; i < index; i++){
		   if (search(symbol[i]) != -1){
			   s.writeByte('E');
			   s.writeShort(reverseOrder(address[i]));
			   s.writeBytes(symbol[i]);
		   }
      }
   }
}




class R extends Endian {
   private short address[];
   private short moduleAddress[];
   private int index;
   private int maxSize;
   public R(int mSize)
   {
      maxSize = mSize;
      address = new short[mSize];
      moduleAddress = new short[mSize];
      index = 0;
   }
   public void  add(short a, short ma) {
      if (index >= maxSize) {
         System.out.println("ERROR: R table overflow");
         System.exit(1);
      }
// MISSING CODE
    	  address[index] = (short) (a); 
    	  moduleAddress[index++] = ma; //we will relocate addresses by their moduleAdresses during phase 2
      
   }
   public short getadd(int i) {
      return address[i];
   }
   public int size() {
      return index;
   }
   public short getma(int i) {
      return moduleAddress[i];
   }
   public void write(DataOutputStream s) throws IOException{
      for (int i = 0; i < index; i++) {
         s.writeByte('R');
         s.writeShort(reverseOrder(address[i]));
      }
   }
}

class S extends Endian {
   private char type;
   private short address;
   private boolean gots;
   public S() {
      gots = false;
   }
   public boolean gots()
   {
      return gots;
   }
   public void add(char c, short a, short ma)
   {
      gots = true;
      type = c;
      if (type == 'S')
         address = a;
      else
         address = (short) (a + ma);
   }
   public void write(DataOutputStream s) throws IOException{
      if (gots()) {
        	   s.writeByte(type);
        	   s.writeShort(reverseOrder(address));
      }
   }
}
class T extends Endian {
   private final int macSize = 4096;
   private short buffer[];
   private int index;
   public T() {
       buffer = new short[macSize];
       index = 0;
   }
   public void add(short x) {
      if (index == macSize) {
         System.out.println("ERROR: Linked program too big");
         System.exit(1);
      }
      buffer[index++] = x;
   }
   public void reloc(int a, int c) {
     buffer[a] =  (short) (buffer[a] & 0xf000 |  (buffer[a] +  c) & 0x0fff);
   }
   public void write(DataOutputStream s) throws IOException{
        for (int i = 0; i <  index; i++)
           s.writeShort(reverseOrder(buffer[i]));
   }
}


public class linv1 extends Endian
{
   boolean ofopen = false;

   String ifilename, ofilename;

   DataOutputStream oStream;
   DataInputStream iStream;

   P pTable;
   E eTable;
   R rTable;
   S sTable;
   T textBuffer;
   short moduleAddress = 0;

       

   public linv1(String[] args) throws IOException {
      pTable = new P(5);
      eTable = new E(5);
      rTable = new R(5);
      sTable = new S();
      textBuffer = new T();
      
      if (args.length == 0) {
         System.out.println("ERROR: Incorrect number of command line arguments");
         System.exit(1);
      }


      for (int argx = 0; argx < args.length; argx++) {
         ifilename = new String(args[argx]);
         doifile();
      }    

      int e;
      for (e = 0; e < eTable.size(); e++) {
         int p = pTable.search(eTable.getsym(e)); //returns address where external symbols are

          if (p >= 0)  
             textBuffer.reloc(eTable.getadd(e), pTable.getadd(p)); 
          else
             break;
      }                                                
      if (e != eTable.size()) {   // no more external references
         System.out.println("ERROR: Unresolved external symbol " +  
         eTable.getsym(e));
            System.exit(1);
      }
    //MISING CODE: Processing associated with R table      
     
      for (int r = 0; r < rTable.size(); r++)
    	  textBuffer.reloc(rTable.getadd(r),rTable.getma(r)); //relocate current address by it's assigned modular address
      
    
      pTable.write(oStream);
      rTable.write (oStream);
      eTable.write(oStream);
      sTable.write(oStream);
      oStream.writeByte('T');
      textBuffer.write(oStream);
      oStream.close();


   }  
      
   public void processfile() throws IOException
   {
     short address;
     char firstChar;
     byte cBuf[] = new byte[120];
     int i;
     	for (;;) //this is equivalent to while(true) ... didn't know that 
     	{                 
        firstChar = (char)iStream.readByte();
        if (firstChar == 'T') {
           try {
              while (true) {
                 textBuffer.add(reverseOrder(iStream.readShort()));
                 moduleAddress++;
              }
           }
           catch (EOFException e) {
              break;
           }
        }


        if (
           firstChar != 'S' && firstChar != 's' &&
           firstChar != 'P' && firstChar != 'E' && firstChar != 'R') {
           System.out.println(
           "ERROR: Input file " + ifilename + " is not linkable");
           System.exit(1);
        }
            
        else {  // S, s, P, E, or R
           address = reverseOrder(iStream.readShort());
           // S
           if (firstChar == 'S' || firstChar == 's') {
              if (sTable.gots()) {
                 System.out.println("ERROR: More than one starting address");
                 System.exit(1);
              }
              sTable.add(firstChar, address, moduleAddress);
            }

            // R
            else if (firstChar == 'R') 
               rTable.add(address, moduleAddress);

            // P or E
            else {
               i = 0; 
               while (true) {
                   cBuf[i] = iStream.readByte();
                   if (cBuf[i++] == '\0') break;
               }
               String stemp = new String(cBuf, 0, i);

               if (firstChar == 'P') 
                  pTable.add(address, moduleAddress, stemp);
               else
               if (firstChar == 'E')
            	   eTable.add(address,moduleAddress,stemp);
//MISING CODE
            }
         }

      } // end of for loop

   }

   public void doifile() throws IOException
   {
      int lastdot, lastslash;

      lastdot = ifilename.lastIndexOf('.');
      lastslash = 
        ifilename.lastIndexOf(System.getProperty("file.separator").charAt(0));
      if (lastdot <= lastslash)
         ifilename = new String(ifilename +".mob");
         try {
            iStream = new DataInputStream(new FileInputStream(ifilename));
         }
         catch (IOException e) {
            System.out.println("ERROR: Cannot open input file " + ifilename);
            System.exit(1);
         }
      
      if (!ofopen) {
         ofopen = true;

         ofilename = new String( 
         ifilename.substring(0,ifilename.lastIndexOf('.')) + ".mac");
         try {
            oStream = new DataOutputStream(new 
                      FileOutputStream(ofilename));
         }
         catch (IOException e) {
            System.out.println(
               "ERROR: Cannot open output file " + ofilename);
            System.exit(1);
         }
       }

       processfile();
       iStream.close();
   }
   public static void main(String[] args) throws IOException
   {
       String author = "linv1 written by Dylan Sheppard"; 
       System.out.println(author); 

       linv1 l = new linv1(args);
   }


}


/*
Output:
client175-120:h1 nyyankees007$ ./ltest java
=======================================================
This shell script compares the model version of linv1
with the student-written version. The first two
test cases should work without errors. The remaining
test cases have errors for which the two linkers
should produce similar error messages.
=======================================================
Hit ENTER to assemble test cases with mas


