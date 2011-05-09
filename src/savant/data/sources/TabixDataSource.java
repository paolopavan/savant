/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.data.sources;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.samtools.util.BlockCompressedInputStream;
import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broad.tabix.TabixReader;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.file.IndexCache;
import savant.data.types.ColumnMapping;
import savant.data.types.TabixIntervalRecord;
import savant.file.DataFormat;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.util.Resolution;

/**
 * DataSource for reading records from a Tabix file.  These can be either a plain Interval
 * records, or full-fledged Bed records.
 *
 * @author mfiume, tarkvara
 */
public class TabixDataSource implements DataSource<TabixIntervalRecord> {
    private static final Log LOG = LogFactory.getLog(TabixDataSource.class);

    TabixReader reader;

    /** Defines mapping between column indices and the data-fields we're interested in. */
    ColumnMapping mapping;

    /** Names of the columns, in the same order they appear in the file. */
    String[] columnNames;

    private URI uri;

    public static TabixDataSource fromURI(URI uri) throws IOException {

        if (uri == null) throw new IllegalArgumentException("Invalid argument: URI must be non-null");

        File indexFile = null;
        // if no exception is thrown, this is an absolute URL
        String scheme = uri.getScheme();
        if ("http".equals(scheme) || "ftp".equals(scheme)) {
            indexFile = getIndexFileCached(uri);
        } else {
            indexFile = getTabixIndexFileLocal(new File(uri));
        }
        if (indexFile != null) {
            SeekableStream baseStream = NetworkUtils.getSeekableStreamForURI(uri);

            TabixReader tr = new TabixReader(baseStream, indexFile);
            return new TabixDataSource(uri, tr);
        }

        // no success
        return null;
    }

    /**
     * Check our source file to see how many columns we have.  If possible, figure
     * out their names.
     *
     * This is only intended as a temporary hack until we get a more flexible DataFormatForm
     * which lets you set up column->field mappings.
     */
    private void inferMapping() throws IOException {
        BlockCompressedInputStream input = new BlockCompressedInputStream(NetworkUtils.getSeekableStreamForURI(uri));
        String line = TabixReader.readLine(input);

        // If we're lucky, the file starts with a comment line with the field-names in it.
        // That's what UCSC puts there, as does Savant.  In some files (e.g. VCF), this
        // magical comment line may be preceded by a ton of metadata comment lines.
        String lastCommentLine = null;
        String commentChar = Character.toString(reader.getCommentChar());
        while (line.startsWith(commentChar)) {
            lastCommentLine = line;
            line = TabixReader.readLine(input);
        }
        input.close();

        String[] fields = line.split("\\t");

        // The chrom, start, and end fields are enough to uniquely determine which of the well-known formats we have.
        if (matchesMapping(ColumnMapping.BED)) {
            // It's a Bed file, but we can set the mapping, because it may have a variable number of actual columns.
            columnNames = new String[] { "chrom", "start", "end", "name", "score", "strand", "thickStart", "thickEnd", null, "blockCount", null, null };
        } else if (fields.length == 10 && matchesMapping(ColumnMapping.GENE)) {
            columnNames = new String[] { "Name", "Chromosome", "Strand", "Transcription start", "Transcription end", "Coding region start", "Coding region end", null, null, null };
            mapping = ColumnMapping.GENE;
        } else if (fields.length == 15 && matchesMapping(ColumnMapping.GENE)) {
            columnNames = new String[] { "Name", "Chromosome", "Strand", "Transcription start", "Transcription end", "Coding region start", "Coding region end", null, null, null, "Unique identifier", "Alternate name", null, null, null };
            mapping = ColumnMapping.GENE;
        } else if (fields.length == 11 && matchesMapping(ColumnMapping.REFSEQ)) {
            columnNames = new String[] { null, "Name ", "Name of chromosome", "Strand", "Transcription start", "Transcription end", "Coding region start", "Coding region end", null, null, null, "Unique identifier", "Alternate name", null, null, null };
            mapping = ColumnMapping.REFSEQ;
        } else if (fields.length == 8 && matchesMapping(ColumnMapping.GFF)) {
            columnNames = new String[] { "Reference", "Program", "Feature", "Start", "End", "Score", "Strand", "Frame", "Group" };
            mapping = ColumnMapping.GFF;
        } else if (fields.length == 21 && matchesMapping(ColumnMapping.PSL)) {
            columnNames = new String[] { "Matches", "Mismatches", "Matches that are part of repeats", "Number of 'N' bases", "Number of inserts in query", "Number of bases inserted in query", "Number of inserts in target", "Number of bases inserted in target", "Strand", "Query sequence name", "Query sequence size", "Alignment start in query", "Alignment end in query", "Target sequence name", "Target sequence size", "Alignment start in target", "Alignment end in target", null, null, null };
            mapping = ColumnMapping.PSL;
        } else if (matchesMapping(ColumnMapping.VCF)) {
            columnNames = new String[] { "Reference", "Position", "ID", "Reference base(s)", "Alternate non-reference alleles", "Quality", "Filter", "Additional information" };
            mapping = ColumnMapping.VCF;
        } else if (fields.length == 4 && matchesMapping(ColumnMapping.GENERIC_INTERVAL)) {
            columnNames = new String[] { "Reference", "Start", "End", "Name" };
            mapping = ColumnMapping.GENERIC_INTERVAL;
        }

        if (mapping == null) {
            // It's either bed, or it's not any format we recognise.
            int name = -1, score = -1, strand = -1, thickStart = -1, thickEnd = -1, itemRGB = -1, blockStarts = -1, blockSizes = -1, name2 = -1;
            boolean bed = false;

            if (lastCommentLine != null) {
                columnNames = lastCommentLine.split("\\t");
            }

            for (int i = 0; i < columnNames.length && i < fields.length; i++) {
                String colName = columnNames[i].toLowerCase();
                if (colName.equals("chrom")) {
                    columnNames[i] = "Reference";
                } else if (colName.equals("start") || colName.equals("chromstart")) {
                    columnNames[i] = "Start";
                } else if (colName.equals("end") || colName.equals("chromend")) {
                    columnNames[i] = "End";
                } else if (colName.equals("name") || colName.equals("feature") || colName.equals("qname")) {
                    name = i;
                    columnNames[i] = "Name";
                } else if (colName.equals("score")) {
                    score = i;
                    columnNames[i] = "Score";
                    bed = true;
                } else if (colName.equals("strand")) {
                    strand = i;
                    columnNames[i] = "Strand";
                    bed = true;
                } else if (colName.equals("thickstart") || colName.equals("cdsstart")) {
                    thickStart = i;
                    columnNames[i] = "Thick start";
                    bed = true;
                } else if (colName.equals("thickend") || colName.equals("cdsend")) {
                    thickEnd = i;
                    columnNames[i] = "Thick end";
                    bed = true;
                } else if (colName.equals("itemrgb") || colName.equals("reserved")) {
                    itemRGB = i;
                    columnNames[i] = null;  // No point in showing colour in the table when we can show it visually.
                    bed = true;
                } else if (colName.equals("blockcount") || colName.equals("exoncount")) {
                    columnNames[i] = "Block count";
                    bed = true;
                } else if (colName.equals("blockstarts") || colName.equals("exonstarts") || colName.equals("tstarts") || colName.equals("chromstarts")) {
                    blockStarts = i;
                    columnNames[i] = null;
                    bed = true;
                } else if (colName.equals("blocksizes") || colName.equals("exonsizes")) {
                    blockSizes = i;
                    columnNames[i] = null;
                    bed = true;
                } else if (colName.equals("name2")) {
                    name2 = i;
                    columnNames[i] = "Alternate name";
                    bed = true;
                }
            }
            if (bed) {
                // We have enough extra columns to justify using a Bed track.
                mapping = ColumnMapping.getBedMapping(reader.getChromColumn(), reader.getStartColumn(), reader.getEndColumn(), name, score, strand, thickStart, thickEnd, itemRGB, -1, blockStarts, -1, blockSizes, name2);
            } else {
                mapping = ColumnMapping.getIntervalMapping(reader.getChromColumn(), reader.getStartColumn(), reader.getEndColumn(), name);
            }
        }
    }

    private boolean matchesMapping(ColumnMapping mapping) {
        return reader.getChromColumn() == mapping.chrom && reader.getStartColumn() == mapping.start && reader.getEndColumn() == mapping.end;
    }

    /**
     * Constructor for internal use only.  Clients should invoke fromURI method.
     */
    TabixDataSource(URI uri, TabixReader reader) throws IOException {
        this.uri = uri.normalize();
        this.reader = reader;

        // Check to see how many columns we actually have, and try to initialise a mapping.
        inferMapping();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TabixIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws OutOfMemoryError {
        List<TabixIntervalRecord> result = new ArrayList<TabixIntervalRecord>();
        try {
            TabixReader.Iterator i = reader.query(MiscUtils.homogenizeSequence(reference) + ":" + range.getFrom() + "-" + (range.getTo()+1));

            if (i != null) {
                String line = null;
                long start = -1;
                long end = -1;
                Map<Long, Integer> ends = new HashMap<Long, Integer>();
                while ((line = i.next()) != null) {
                    //Note: count is used to uniquely identify records in same location
                    //Assumption is that iterator will always give records in same order
                    TabixIntervalRecord tir = TabixIntervalRecord.valueOf(line, mapping);
                    if(tir.getInterval().getStart() == start){
                        end = tir.getInterval().getEnd();
                        if(ends.get(end) == null){
                            ends.put(end, 0);
                        } else {
                            int count = ends.get(end)+1;
                            ends.put(end, count);
                            tir.setCount(count);
                        }
                    } else {
                        start = tir.getInterval().getStart();
                        end = tir.getInterval().getEnd();
                        //FIXME: is the overhead of doing this high?
                        ends = new HashMap<Long, Integer>();
                        ends.put(end, 0);
                        tir.setCount(0);
                    }
                    result.add(tir);
                }
            }
        } catch (IOException ex) {
            LOG.error(ex);
        }
        return result;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {}

    @Override
    public Set<String> getReferenceNames() {
        return reader.getReferenceNames();
    }

    private static File getTabixIndexFileLocal(File tabixFile) {
        String tabixPath = tabixFile.getAbsolutePath();
        File indexFile = new File(tabixPath + ".tbi");
        if (indexFile.exists()) {
            return indexFile;
        } else {
            // Try alternate index file name.
            indexFile = new File(tabixPath.replace(".gz", ".tbi"));
            if (indexFile.exists()) {
                return indexFile;
            }
        }
        return null;
    }

    private static File getIndexFileCached(URI tabixURI) throws IOException {
        return IndexCache.getInstance().getIndex(tabixURI, "tbi", "gz");
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public String getName() {
        return MiscUtils.getNeatPathFromURI(getURI());
    }

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.TABIX;
    }

    @Override
    public Object getExtraData() {
        return null;
    }

    /**
     * Tabix can hold data which is actually INTERVAL_GENERIC or INTERVAL_BED.
     */
    public final DataFormat getEffectiveDataFormat() {
        return mapping.format;
    }

    public final String[] getColumnNames() {
        return columnNames;
    }
}
