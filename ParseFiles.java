import java.io.*;
import java.util.*;

public class ParseFiles {
    ArrayList<String> dsspFile;
    ArrayList<String> pdbFile;

    public ParseFiles (ArrayList<String> dsspFile, ArrayList<String> pdbFile) {
        this.dsspFile = dsspFile;
        this.pdbFile = pdbFile;
    }
    
    public ArrayList<Residue> getInfoFromPDB(ArrayList<String> pdbFile) {
    	
    	CartesianCoord coords;
    	int pdbResNum;
    	
    	for (int i = 0; i<pdbFile.size(); ++i) {		
    		// do I really need String line = "";
    		if(pdbFile.get(i).substring(0,6).trim().equals("ATOM")) {
    			String[] strs = customPDBSplit(pdbFile.get(i));
    			coords = getCoordinates(strs);
    			pdbResNum = strs[3];
    		}
    	}
    }
    
    //this method splits a pdb file line into 
	public String[] customPDBSplit(String splitMe) {
		ArrayList<String> strArrayList = new ArrayList<String>();
		strArrayList.add(splitMe.substring(0,6).trim()); //    0   ATOM designation
		strArrayList.add(splitMe.substring(11,16).trim()); //  1   atom name type
		strArrayList.add(splitMe.substring(17,20).trim()); //  2   residue name
		strArrayList.add(splitMe.substring(22,26).trim()); //  3   residue sequence number
		strArrayList.add(splitMe.substring(30,38).trim()); //  4   x
		strArrayList.add(splitMe.substring(38,46).trim()); //  5   y
		strArrayList.add(splitMe.substring(47,54).trim()); //  6   z
		strArrayList.add(splitMe.substring(54,60).trim()); //  7   occupancy
		strArrayList.add(splitMe.substring(60,66).trim()); //  8   temperature factor
		
		return strArrayList.toArray();
	}
   
    //At indices 4, 5, and 6 are where the xyz coordinates are stored, accounting for zero indexing.
    public CartesianCoord getCoordinates(String[] strs){
    	double x = Double.parseDouble(strs[4]); 
    	double y = Double.parseDouble(strs[5]);
    	double z = Double.parseDouble(strs[6]);
    	return new CartesianCoord(x,y,z);
    }
    
    public ArrayList<String> getCoordinates(ArrayList<String> rawPdbFile) {
		ArrayList<String> coordinatesOfAtoms = new ArrayList<String>();
		for (int i = 0; i < rawPdbFile.size(); i++) {
			String[] strs = rawPdbFile.get(i).split("\\s+");
			String line = "";
			if (strs[0].equals("ATOM")) {
				//collect atom name, residue, residue ID #, and X, Y, Z coordinates in a single string
				line = strs[0] + " " + strs[2] + " " + strs[3] + " " + strs[5] + "" + 
			               strs[6] + " " + strs[7] + " " + strs[8];
				coordinatesOfAtoms.add(line);
			}
			else if (strs[0].equals("TER"))
				//C-terminus flag
				line = strs[0] + " " + strs[2] + " " + strs[4];
				coordinatesOfAtoms.add(line);
			}
		}
		return coordinatesOfAtoms;
	}
    
	public double getTotalMean(ArrayList<String> rawPdbFile) {
		// calculate mean b-factor and std deviation b-factors for whole
		// protein
		int countTotalAtoms = 0;
		double totalbFactor = 0;
		for (int i = 0; i < rawPdbFile.size(); i++) {
			String[] strs = rawPdbFile.get(i).split("\\s+");
			if (strs[0].equals("ATOM")) {
				countTotalAtoms++;
				double strbFactortoDbl = Double.valueOf(strs[10].trim())
						.doubleValue();
				totalbFactor += strbFactortoDbl;
			}
		}
		double meanbFactor = totalbFactor / countTotalAtoms;
		return meanbFactor;
	}

	public double getTotalStdDev(ArrayList<String> rawPdbFile) {
		int countTotalAtoms = 0;
		double totalbFactor = 0;
		double totalsquaredbFactor = 0;
		for (int i = 0; i < rawPdbFile.size(); i++) {
			String[] strs = rawPdbFile.get(i).split("\\s+");
			if (strs[0].equals("ATOM")) {
				countTotalAtoms++;
				double strbFactortoDbl = Double.valueOf(strs[10].trim())
						.doubleValue();
				double squaredbFactor = Math.pow(strbFactortoDbl, 2);
				totalbFactor += strbFactortoDbl;
				totalsquaredbFactor += squaredbFactor;
			}
		}
		double StdDev = calcStdDev(totalbFactor, totalsquaredbFactor,
				countTotalAtoms);
		return StdDev;
	}

	public ArrayList<Double> calculateBfactorZScore(ArrayList<String> rawPdbFile,
			double totalMean, double totalStdDev) {
		ArrayList<Double> zScoresOfPDBFile = new ArrayList<Double>();
		int countTotalAtoms = 0;
		int currentResidue = 0;
		int pastResidue = 0;
		double currentResidueBfactor = 0;
		long countResidues = 0;
		double meanOfCurrentResidue = 0;
		for (int i = 0; i < rawPdbFile.size(); i++) {
			String[] strs = rawPdbFile.get(i).split("\\s+");
			if (strs[0].equals("ATOM")) { // only want lines from PDB that are
											// from ATOM section
				// calculate total B-factor of a residue
				if (countTotalAtoms == 0) {
					pastResidue = Integer.parseInt(strs[5]);
					countResidues++;
					currentResidueBfactor += Double.valueOf(strs[10].trim()).doubleValue();
				} else {
					currentResidue = Integer.parseInt(strs[5]);
					if (currentResidue == pastResidue) {
						countResidues++;
						currentResidueBfactor += Double
								.valueOf(strs[10].trim()).doubleValue();
					} else {
						meanOfCurrentResidue = currentResidueBfactor
								/ (countResidues); // calculate mean B-factor of a residue
						zScoresOfPDBFile.add(zScore(meanOfCurrentResidue, totalMean, totalStdDev));
						countResidues = 0;
						pastResidue = currentResidue;
						currentResidueBfactor = 0;
						currentResidueBfactor += Double.valueOf(strs[10].trim()).doubleValue();
					}
				}
			}
		}
		return zScoresOfPDBFile;
	}
	
	public double zScore(double currentMean, double totalMean, double totalStdDev) {
		double zScore = (currentMean - totalMean) / totalStdDev;
		return zScore;
	}

    
    public double calcStdDev(double total, double squaredTotal, int counter) {
    	double firstTerm = squaredTotal/(counter-1);
    	double secondTerm = (counter/(counter-1));
    	double thirdTerm = Math.pow((total/counter), 2);
    	double stdDev = Math.sqrt(firstTerm-(secondTerm*thirdTerm));
    	return stdDev;
    }
    
    public ArrayList<SecondaryStructure> extractSS(ArrayList<String> dsspFile) {
    	ArrayList<Residue> tempArray = new ArrayList<Residue>();
    	
    	String[] sheetArray = {"E","B"};
    	String[] helixArray = {"G","H","I"};
    	String[] turnArray = {" ","S","T"};
    	
    	for (int i = 0; i<dsspFile.size(); ++i) {
		    tempArray.get(i) = new Residue();
		    if(charsAtEqual(dsspFile, i, 14, sheetArray)) {
				tempArray.get(i).setSSType("S");
		    }
		    else if(charsAtEqual(dsspFile, i, 14, helixArray)) {
				tempArray.get(i).setSSType("H");
		    }
		    else if(charsAtEqual(dsspfile, i, 14, turnArray)) {
				tempArray.get(i).setSSType("T");
		    }
    	}
    }
    
    public boolean charAtEquals(ArrayList<String> file, int index, int num, String charac) {
    	return file.get(index).charAt(num).equals(charac);
    }
    
    public boolean charsAtEqual(ArrayList<String> file, int index, int num, String[] chars) {
    	for (int j=0; j<chars.size(); ++j){
	   		if(!charAtEquals(file, index, num, chars.get(j))) {
				return false;
	    	}    	
    	}
    	return true;
    }
    
    public void printOut(ArrayList<String> al) {
    	for (int i=0; i<al.size(); ++i){
    	    System.out.println(al.get(i));
    	}
    }
    
    public static void main(String[] args){
    }
}