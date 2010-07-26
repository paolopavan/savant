/*
 *    Copyright 2010 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/*
 * SavantFileFormatterUtils.java
 * Created on Jan 12, 2010
 */

package savant.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import savant.format.header.FileType;
import savant.format.header.FileTypeHeader;
import savant.format.util.data.FieldType;
import savant.model.BEDIntervalRecord;
import savant.model.GenericIntervalRecord;
import savant.model.Interval;
import savant.model.IntervalRecord;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Utilities for manipulating data files.
 * @author vwilliams
 */
public class SavantFileFormatterUtils {

    // Constants to use for size calculations on output fields.
    public static final int LONG_FIELD_SIZE     = 8;
    public static final int INT_FIELD_SIZE      = 4;
    public static final int SHORT_FIELD_SIZE    = 2;
    public static final int CHAR_FIELD_SIZE     = 2;
    public static final int BYTE_FIELD_SIZE     = 1;
    public static final int BOOLEAN_FIELD_SIZE  = 1;
    public static final int DOUBLE_FIELD_SIZE   = 8;
    public static final int FLOAT_FIELD_SIZE    = 4;

    public static FileType getTrackDataTypeFromPath(String path) {

        // get the file extension
        String extension = getExtension(path);
        extension = extension.toUpperCase();

        if (extension.equals("BAM")) {
            return FileType.INTERVAL_BAM;
        }
//        if (extension.equals("SAVANT")) {
//            return FileType.SAVANT;
//        }

        return null;
    }
    
    /**
     * Extract the extension from the given path
     * @param path The path from which to extract the extension
     * @return The extension of the file at the given path
     */
    public static String getExtension(String path) {
        int indexOfDot = path.lastIndexOf(".");

        if (indexOfDot == -1 || indexOfDot == path.length() - 1) {
            return "";
        } else {
            return path.substring(indexOfDot + 1);
        }
    }


    // TODO: remove, not used
    public static FileTypeHeader readFileTypeHeader(DataInputStream in) throws IOException {
        List<FieldType> fields = new ArrayList<FieldType>();
        fields.add(FieldType.INTEGER);
        fields.add(FieldType.INTEGER);
        List<Object> record = readBinaryRecord(in,fields);
        FileTypeHeader fth = new FileTypeHeader( FileType.fromMagicNumber((Integer) record.get(0)), (Integer) record.get(1));
        return fth;
    }



    // TODO: remove, not used
    public static List<Object> readBinaryRecord(DataInputStream in, List<FieldType> fields) throws IOException {

        List<Object> record = new ArrayList<Object>(fields.size());

        //System.out.println("Reading binary record");
        //System.out.println("Fields");
        //for (FieldType ft : fields) {
        //    System.out.println("\t" + ft);
        // }

        for (FieldType ft : fields) {
            if (ft == FieldType.IGNORE) { continue; }

            switch(ft) {
                case INTEGER:
                    record.add(in.readInt());
                    break;
                case ITEMRGB:
                    int r = in.readInt();
                    int g = in.readInt();
                    int b = in.readInt();
                    record.add(new ItemRGB(r,g,b));
                    break;
                case DOUBLE:
                    record.add(in.readDouble());
                    break;
                case BLOCKS:
                    int numBlocks = in.readInt();
                    List<Block> blocks = new ArrayList<Block>(numBlocks);
                    for (int i = 0; i < numBlocks; i++) {
                        int start = in.readInt();
                        int size = in.readInt();
                        blocks.add(new Block(start,size));
                    }
                    record.add(blocks);
                    break;
                case STRING:
                    int len = in.readInt();
                    String s = "";
                    for (int i = 0; i < len; i++) {
                        s += (char) in.readByte();
                    }
                    record.add(s);
                    break;
                case CHAR:
                    record.add((char) in.readByte());
                    break;
                case RANGE:
                    int start = in.readInt();
                    int end = in.readInt();
                    record.add(new Range(start,end));
                    break;
                case LONG:
                    record.add(in.readLong());
                    break;
                case IGNORE:
                    break;
                default:
                    System.err.println("Not implemented yet for Field Type: " + ft);
                    break;
            }
        }

        return record;
    }

    /** IO **/

    /**
     * Write this line as binary to outFile
     * @param outFile
     * @param line
     * @param fields
     * @param modifiers
     * @throws IOException
     */
    public static void writeBinaryRecord(DataOutputStream outFile, List<Object> line, List<FieldType> fields, List<Object> modifiers) throws IOException {

        //System.out.println("Writing records with fields:");
        for (int i = 0; i < line.size(); i++) {
            //System.out.println("\t" + (i+1) + ". " + fields.get(i) + "\t" + line.get(i));
        }

//        FieldType fieldType = null;
        Object o = null;
        Object instruction = null;

        int entryNum = 0;
        int numIgnores = 0;

        for (FieldType ft : fields) {
            if (ft == FieldType.IGNORE) {
                numIgnores++;
                continue;
            }

            o = line.get(entryNum);
            instruction = modifiers.get(entryNum + numIgnores);

            //System.out.println("Writing " + o + " as " + ft + " with instruction " + instruction);

            switch (ft) {
                case STRING:
                    int stringLength;
                    if (modifiers.get(entryNum) == null) {
                        stringLength = ((String) o).length();
                    } else {
                        stringLength = (Integer) instruction;
                    }
                    writeFixedLengthString(outFile, (String) o, stringLength);
                    break;
                case CHAR:
                    outFile.writeByte((Character) o);
                    break;
                case BLOCKS:
                    List<Block> blocks = (List<Block>) o;
                    outFile.writeInt(blocks.size());
                    for (Block b : blocks) {
                        outFile.writeInt(b.position);
                        outFile.writeInt(b.size);
                    }
                    break;
                case ITEMRGB:
                    ItemRGB rgb = (ItemRGB) o;
                    outFile.writeInt(rgb.red);
                    outFile.writeInt(rgb.blue);
                    outFile.writeInt(rgb.green);
                    break;
                case INTEGER:
                    outFile.writeInt((Integer) o);
                    break;
                case DOUBLE:
                    outFile.writeDouble((Double) o);
                    break;
                case FLOAT:
                    outFile.writeFloat((Float) o);
                    break;
                case LONG:
                    outFile.writeLong((Long) o);
                    break;
                case BOOLEAN:
                    int bool = Integer.parseInt((String) o);
                    outFile.writeInt(bool);
                    break;
                case RANGE:
                    Range r = (Range) o;
                    outFile.writeInt(r.getFrom());
                    outFile.writeInt(r.getTo());
                    break;
                default:
                    System.err.println("DataFormatUtils.writeBinaryRecord: Not implemented for " + ft);
                    break;
            }

            entryNum++;
        }
    }

    /**
     * Parse next line in the inFile according to given field types
     * @param txtLine - String line of text
     * @param fields
     * @return
     * @throws IOException
     */
    public static List<Object> parseTxtLine(String txtLine, List<FieldType> fields) throws IOException {

        if (txtLine == null) {
            return null;
        }

        //String delimiter = "\\s+";
        String delimiter = "( |\t)+";

        //StringTokenizer tok = new StringTokenizer(txtLine,"\t");
        //StringTokenizer tok = new StringTokenizer(txtLine," ");

        String[] s = txtLine.split(delimiter);

        //int tokenNum = 0;
        List<Object> line = new ArrayList<Object>(fields.size());

        String token = "";

        //NEW
        for(int i = 0; i < Math.max(fields.size(), s.length); i++){

            token = s[i];
            FieldType ft = fields.get(i);

            switch (fields.get(i)) {
                case STRING:
                    line.add(token);
                    break;
                case CHAR:
                    line.add(token.charAt(0));
                    break;
                case INTEGER:
                    line.add(Integer.parseInt(token));
                    break;
                case DOUBLE:
                    line.add(Double.parseDouble(token));
                    break;
                case FLOAT:
                    line.add(Float.parseFloat(token));
                    break;
                case BOOLEAN:
                    line.add(Integer.parseInt(token));
                    break;
                case ITEMRGB:
                    ItemRGB rgb = parseItemRGB(token);
                    line.add(rgb);
                    break;
                case BLOCKS:
                    int numBlocks = Integer.parseInt(token);
                    String blockSizes = s[i+1];
                    String blockPositions = s[i+2];
                    List<Block> blocks = parseBlocks(numBlocks,blockPositions,blockSizes);
                    line.add(blocks);
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IOException("Unrecognized field type: " + ft);
            }
        }
        //END NEW




        /*while (tok.hasMoreElements() && tokenNum < fields.size()) {

            token = tok.nextToken();
            FieldType ft = fields.get(tokenNum);

            //System.out.println("Parsing " + token + " as " + ft);

            switch (fields.get(tokenNum)) {
                case STRING:
                    line.add(token);
                    break;
                case CHAR:
                    line.add(token.charAt(0));
                    break;
                case INTEGER:
                    line.add(Integer.parseInt(token));
                    break;
                case DOUBLE:
                    line.add(Double.parseDouble(token));
                    break;
                case FLOAT:
                    line.add(Float.parseFloat(token));
                    break;
                case BOOLEAN:
                    line.add(Integer.parseInt(token));
                    break;
                case ITEMRGB:
                    ItemRGB rgb = parseItemRGB(token);
                    line.add(rgb);
                    break;
                case BLOCKS:
                    int numBlocks = Integer.parseInt(token);
                    String blockSizes = tok.nextToken();
                    String blockPositions = tok.nextToken();
                    List<Block> blocks = parseBlocks(numBlocks,blockPositions,blockSizes);
                    line.add(blocks);
                    break;
                case IGNORE:
                    break;
                default:
                    throw new IOException("Unrecognized field type: " + ft);
            }
            tokenNum++;
        }*/

        return line;
    }

        /** HEADERS **/
    /**
     * FILE TYPE HEADER
     * @throws IOException
     */
    public static void writeFileTypeHeader(DataOutputStream outFile, FileTypeHeader fth) throws IOException {
        outFile.writeInt(fth.fileType.getMagicNumber());
        outFile.writeInt(fth.version);
    }

    /**
     * FIELDS HEADER
     * @param fields
     * @throws IOException
     */
    public static void writeFieldsHeader(DataOutputStream outFile, List<FieldType> fields) throws IOException {

        if (fields == null) {
            outFile.writeInt(0);
            return;
        }

        outFile.writeInt(fields.size());
        
        for (FieldType ft : fields) {
            outFile.writeInt(ft.ordinal());
        }
    }
    


    public static void printRecord(List<Object> record) {
        for (Object o : record) {
            System.out.print(o + "\t");
        }
        System.out.println();
    }


    public static int getRecordSize(List<Object> record, List<FieldType> fields) {

        int recordSize = 0;

        int recIndex = 0;
        for (FieldType ft : fields) {
            if  (ft == FieldType.IGNORE) { continue; }

            switch(ft) {
                case STRING:
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE + ((String) record.get(recIndex)).length()* SavantFileFormatterUtils.BYTE_FIELD_SIZE;
                    break;
                case INTEGER:
                    recordSize +=  SavantFileFormatterUtils.INT_FIELD_SIZE;
                    break;
                case ITEMRGB:
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE*3;
                    break;
                case BLOCKS:
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE*((List<Block>) record.get(9)).size();
                    break;
                case CHAR:
                    recordSize +=  SavantFileFormatterUtils.BYTE_FIELD_SIZE;
                    break;
                case DOUBLE:
                    recordSize += SavantFileFormatterUtils.DOUBLE_FIELD_SIZE;
                    break;
                case FLOAT:
                    recordSize += SavantFileFormatterUtils.FLOAT_FIELD_SIZE;
                    break;
                case BOOLEAN:
                    // TODO: change?!
                    recordSize += SavantFileFormatterUtils.INT_FIELD_SIZE;
                    break;
                default:
                    throw new UnsupportedOperationException("Data Utils.getRecordSize: Not implemented yet!");
            }

            recIndex++;
        }

        return recordSize;
    }

    public static IntervalRecord convertRecordToInterval(List<Object> record, FileType fileType, List<FieldType> fields) {
        IntervalRecord ir = null;
        switch(fileType) {
            case INTERVAL_GENERIC:
                ir = convertRecordToGenericInterval(record,fields);
                break;
            case INTERVAL_BED:
                ir = convertRecordToBEDInterval(record,fields);
                break;
            case INTERVAL_GFF:

                //TODOL fix... this won't work unless indexes correspond to start / end indicies
                ir = convertRecordToGenericInterval(record,fields);
                break;
            default:
                break;
        }

        return ir;
    }

    private static IntervalRecord convertRecordToGenericInterval(List<Object> record, List<FieldType> fields) {
        GenericIntervalRecord ir = new GenericIntervalRecord((String) record.get(0), new Interval((Integer) record.get(1),(Integer) record.get(2)), (String) record.get(3));
        return (IntervalRecord) ir;
    }

    // TODO: make it actually return a GFF IntervalRecord
    private static IntervalRecord convertRecordToGFFInterval(List<Object> record, List<FieldType> fields) {
        GenericIntervalRecord ir = new GenericIntervalRecord((String) record.get(0), new Interval((Integer) record.get(3),(Integer) record.get(4)), (String) record.get(1));
        return (IntervalRecord) ir;
    }

    private static IntervalRecord convertRecordToBEDInterval(List<Object> record, List<FieldType> fields) {

        Interval interval = new Interval((Integer) record.get(1), (Integer) record.get(2));
        String ref = (String) record.get(0);
        
        int numFields = record.size();
        
        String name = "";
        Integer score = 0;
        Strand strand = SavantFileFormatterUtils.getStrand("+");
        Integer thickStart = 0;
        Integer thickEnd = 0;
        ItemRGB rgb = new ItemRGB(0,0,0);
        List<Block> blocks = null;
        
        if (numFields > 3) { name = (String) record.get(3); }
        if (numFields > 4) { score = (Integer) record.get(4); }
        if (numFields > 5) { strand = SavantFileFormatterUtils.getStrand((String) record.get(5)); }
        if (numFields > 6) { thickStart = (Integer) record.get(6); } else { thickStart = 0; }
        if (numFields > 7) { thickEnd = (Integer) record.get(7); } else { thickEnd = interval.getLength(); }
        if (numFields > 8) { rgb = (ItemRGB) record.get(8); }
        if (numFields > 9) { blocks = (List<Block>) record.get(9); } else {
            blocks = new ArrayList<Block>();
            blocks.add(new Block(0,interval.getLength()));
        }

        BEDIntervalRecord ir = new BEDIntervalRecord(
                ref,
                interval,
                name,
                score,
                strand,
                thickStart,
                thickEnd,
                rgb,
                blocks
                );
        
       return ir;
    }

    private static List<Block> parseBlocks(int numBlocks, String blockPositions, String blockSizes) {

        List<Block> blocks = new ArrayList<Block>(numBlocks);

        if (numBlocks > 0) {
            StringTokenizer posTokenizer = new StringTokenizer(blockPositions,",");
            StringTokenizer sizeTokenizer = new StringTokenizer(blockSizes,",");

            while(numBlocks-- > 0) {
                int nextPos = Integer.parseInt(posTokenizer.nextToken());
                int nextSize = Integer.parseInt(sizeTokenizer.nextToken());
                blocks.add(new Block(nextPos,nextSize));
            }
        }

        return blocks;
    }

    public static Strand getStrand(String strand) {
        char c = strand.charAt(0);
        if (c == '-') {
            return Strand.REVERSE;
        } else {
            return Strand.FORWARD;
        }
    }

    // TODO: actually parse
    private static ItemRGB parseItemRGB(String token) {
        return new ItemRGB(0,0,0);
    }

    public static void writeString(DataOutputStream out, String s) throws IOException {
        writeFixedLengthString(out,s,s.length());
    }

    public static void writeFixedLengthString(DataOutputStream out, String s, int len) throws IOException {

        int pad = len - s.length();

        out.writeInt(len);
        if (!s.equals("")) { out.writeBytes(s.substring(0, Math.min(s.length(),len))); }
        while (pad > 0) {
            out.writeBytes(" ");
            pad--;
        }

        //System.out.println("\tWriting " + len + " chars from [" + s + "] padded by " + (len - s.length()) + " = " + (after-before) + " bytes");
    }


    public static Map<String, String> splitFile(String filePath, int columnNumber ) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));

        Map<String, String> seqnameToFileNameMap = new HashMap<String,String>();
        Map<String, BufferedWriter> seqnameToBufferedWriterMap = new HashMap<String,BufferedWriter>();

        BufferedWriter bw;

        String line = "";
        while((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line,"\t");
            for (int i = 0; i < columnNumber; i++) {
                st.nextToken();
            }
            
            String t = st.nextToken();

            if (seqnameToFileNameMap.containsKey(t)) {
                bw = seqnameToBufferedWriterMap.get(t);
            } else {
                String fn = filePath + ".split_" + t;
                bw = new BufferedWriter(new FileWriter(fn));
                seqnameToFileNameMap.put(t,fn);
                seqnameToBufferedWriterMap.put(t,bw);
            }

            bw.write(line + "\n");
        }
        
        br.close();


        for (BufferedWriter bwr : seqnameToBufferedWriterMap.values()) {
            bwr.flush();
            bwr.close();
        }

        //List<String> outFiles = new ArrayList<String>();

        //for (String v : seqnameToFileNameMap.values()) {
        //    outFiles.add(v);
        //}

        //return outFiles;

        return seqnameToFileNameMap;
    }
}