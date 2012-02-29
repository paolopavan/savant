/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.ms;

import org.broad.igv.feature.genome.Genome.ChromosomeComparator;
import org.ut.biolab.medsavant.db.api.MedSavantDatabase.DefaultVariantTableSchema;
import savant.api.data.PointRecord;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;

/**
 *
 * @author Andrew
 */
public class MedSavantVariantRecord implements VariantRecord {
    
    private String chrom;
    private int position;
    private String ref;
    private String alt;    
    private org.ut.biolab.medsavant.vcf.VariantRecord.VariantType type;
    private int index;

    public MedSavantVariantRecord(Object[] arr, int index) {
        this.chrom = (String)arr[DefaultVariantTableSchema.INDEX_OF_CHROM];
        this.position = (Integer)arr[DefaultVariantTableSchema.INDEX_OF_POSITION];
        this.ref = (String)arr[DefaultVariantTableSchema.INDEX_OF_REF];
        this.alt = (String)arr[DefaultVariantTableSchema.INDEX_OF_ALT];
        this.type = org.ut.biolab.medsavant.vcf.VariantRecord.VariantType.valueOf((String)arr[DefaultVariantTableSchema.INDEX_OF_VARIANT_TYPE]);
        this.index = index;
    }
    
    @Override
    public VariantType getVariantType() {
        switch(type){
            case Insertion:
                return VariantType.INSERTION;
            case Deletion:
                return VariantType.DELETION;
            case SNP:
                if(alt != null && alt.length() > 0){
                    String a = alt.substring(0, 1).toLowerCase();
                    if(a.equals("a")){
                        return VariantType.SNP_A;
                    } else if (a.equals("c")){
                        return VariantType.SNP_C;
                    } else if (a.equals("g")){
                        return VariantType.SNP_G;
                    } else if (a.equals("t")){
                        return VariantType.SNP_T;
                    }
                }
            default:
                return VariantType.OTHER;
        }
    }

    @Override
    public String getRefBases() {
        return ref;
    }

    @Override
    public String[] getAltAlleles() {
        return alt.split(",");
    }

    @Override
    public int getParticipantCount() {
        return 1;
    }

    @Override
    public VariantType[] getVariantsForParticipant(int index) {
        if(index == this.index){
            return new VariantType[]{getVariantType()};
        } else {
            return null;
        }
    }

    @Override
    public int[] getAllelesForParticipant(int index) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getPosition() {
        return position;
    }

    @Override
    public String getName() {
        return chrom + ":" + position;
    }

    @Override
    public String getReference() {
        return chrom;
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof PointRecord)) return -1;
        PointRecord other = (PointRecord)o;
        int chromCompare = (new ChromosomeComparator()).compare(chrom, other.getReference());
        if(chromCompare != 0) return chromCompare;
        return ((Integer)position).compareTo(other.getPosition());
    }
    
    public String toString(){
        return getName();
    }
    
}