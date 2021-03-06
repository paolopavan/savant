/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import savant.api.util.Listener;
import savant.file.FileType;
import savant.format.FormatEvent;
import savant.format.SavantFileFormatter;
import savant.format.SavantFileFormatterUtils;
import savant.format.SavantFileFormattingException;


/**
 * Command-line format utility.
 *
 * @author tarkvara
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class FormatTool {

    public static void main(String args[]) {
        try {
            File inFile = null;
            File outFile = null;
            FileType ft = null;
            boolean oneBased = false;
            boolean forceOneBased = false;
            String typeStr = null;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-t")) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("File type not specified.");
                    }
                    typeStr = args[++i];
                } else if (args[i].equals("-1")) {
                    forceOneBased = true;
                    oneBased = true;
                } else if (inFile == null) {
                    inFile = new File(args[i]);
                } else if (outFile == null) {
                    outFile = new File(args[i]);
                } else {
                    throw new IllegalArgumentException(String.format("Unrecognised command line argument: %s.", args[i]));
                }
            }

            if (typeStr != null) {
                ft = parseFileType(typeStr);
            }

            if (inFile == null) {
                throw new IllegalArgumentException("Input file not specified.");
            }
            if (!inFile.exists()) {
                throw new FileNotFoundException(String.format("File not found: %s.", inFile.getAbsolutePath()));
            }
            if (ft == null) {
                ft = SavantFileFormatterUtils.guessFileTypeFromPath(inFile.getAbsolutePath());
                if (ft == null) {
                    ft = FileType.INTERVAL_UNKNOWN;
                    System.out.println(String.format("Unable to determine type of %s; will try to infer fields from comment on first line.", inFile.getName()));
                }
            }
            if (!forceOneBased) {
                oneBased = inferOneBased(ft);
            }
            if (outFile == null) {
                switch (ft) {
                    case INTERVAL_GENERIC:
                    case INTERVAL_BED:
                    case INTERVAL_BED1:
                    case INTERVAL_GFF:
                    case INTERVAL_GTF:
                    case INTERVAL_PSL:
                    case INTERVAL_VCF:
                    case INTERVAL_KNOWNGENE:
                    case INTERVAL_REFGENE:
                    case INTERVAL_UNKNOWN:
                        outFile = new File(inFile.getAbsolutePath() + ".gz");
                        break;
                    case CONTINUOUS_GENERIC:
                    case CONTINUOUS_WIG:
                        outFile = new File(inFile.getAbsolutePath() + ".tdf");
                        break;
                    default:
                        outFile = new File(inFile.getAbsolutePath() + ".savant");
                        break;
                }
            }
            try {
                SavantFileFormatter sff = SavantFileFormatter.getFormatter(inFile, outFile, ft);
                sff.addListener(new Listener<FormatEvent>() {
                    @Override
                    public void handleEvent(FormatEvent event) {
                        if (event.getType() == FormatEvent.Type.PROGRESS && event.getSubTask() != null) {
                            System.out.println(event.getSubTask());
                        }
                    }
                });
                sff.format();
            } catch (InterruptedException ix) {
                System.err.println("Formatting interrupted.");
            } catch (IOException iox) {
                System.err.println("Fatal I/O error.");
                System.err.println(iox.getMessage());
            } catch (SavantFileFormattingException sffx) {
                System.err.println(sffx.getMessage());
            }
        } catch (Exception x) {
            // We get here for usage exceptions.  Actual processing exceptions are
            // all caught inside the block.
            System.err.println(x.getMessage());
            System.err.println();
            usage();
        }
    }

    private static FileType parseFileType(String arg) {
        String s = arg.toLowerCase();
        if (s.equals("fasta")) {
            return FileType.SEQUENCE_FASTA;
        }
        if (s.equals("bed")) {
            return FileType.INTERVAL_BED;
        }
        if (s.equals("bed1")) {
            return FileType.INTERVAL_BED1;
        }
        if (s.equals("gff")) {
            return FileType.INTERVAL_GFF;
        }
        if (s.equals("gtf")) {
            return FileType.INTERVAL_GTF;
        }
        if (s.equals("bam")) {
            return FileType.INTERVAL_BAM;
        }
        if (s.equals("wig") || s.equals("bedgraph")) {
            return FileType.CONTINUOUS_WIG;
        }
        if (s.equals("interval")) {
            return FileType.INTERVAL_GENERIC;
        }
        if (s.equals("point")) {
            return FileType.POINT_GENERIC;
        }
        if (s.equals("continuous")) {
            return FileType.CONTINUOUS_GENERIC;
        }
        if (s.equals("psl")) {
            return FileType.INTERVAL_PSL;
        }
        if (s.equals("vcf")) {
            return FileType.INTERVAL_VCF;
        }
        if (s.equals("gene") || s.equals("knowngene")) {
            return FileType.INTERVAL_KNOWNGENE;
        }
        if (s.equals("refgene")) {
            return FileType.INTERVAL_REFGENE;
        }
        throw new IllegalArgumentException(String.format("Unknown file type: %s.", arg));
    }

    private static boolean inferOneBased(FileType ft) {
        switch (ft) {
            case SEQUENCE_FASTA:
            case INTERVAL_GFF:
            case INTERVAL_GTF:
            case INTERVAL_BAM:
            case CONTINUOUS_WIG:
                return true;
            case INTERVAL_BED:
                return false;
            default:
                // For the generic formats, if they didn't specify "-1", they must mean zero-based.
                return false;
        }
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private static void usage() {
        System.err.println("Usage: FormatTool [-t type] [-1] inFile [outFile]");
        System.err.println("    -t       file type (one of FASTA, BED, GFF, BAM, WIG, BedGraph, Interval,");
        System.err.println("             Point, Continuous, Gene; if omitted, will try to infer from file");
        System.err.println("             extension)");
        System.err.println("    -1       treat the file as one-based (default for FASTA, GFF, BAM, WIG, and");
        System.err.println("             BedGraph)");
        System.err.println("    inFile   the unformatted input file (required)");
        System.err.println("    outFile  the output file (if omitted, will default to inFile.gz or inFile.tdf)");
    }
}
