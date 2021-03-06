import java.io.*;
import java.util.*;
import java.lang.*;
import Jama.*;
//  currently compiles mostly

public class AtomToResidue {
	
	//takes as input an unsorted arraylist of atoms
	public AtomToResidue(ArrayList<Atom> al, ArrayList<String> dsspFile, double bFactorMean, double bFactorSTD) {
		ArrayList<Atom> atomList = sortAtoms(al);
		ArrayList<Residue> resArray = turnIntoResidueArray(atomList, dsspFile, bFactorMean, bFactorSTD);
	}
	
	public ArrayList<Atom> sortAtoms(ArrayList<Atom> atomList) {
		Collections.sort(atomList, new AtomComparator());
	}
    
	public ArrayList<Residue> sortResidues(ArrayList<Residue> resList) {
		Collections.sort(resList, new ResidueComparator());
	}
    
	public double getAtomicWeight(Atom currentAtom) { //NEW PMoI method to get atomic weight given atom type
		double atomWeight = 0;
		String atomType = Character.toString(currentAtom.getAtomType().charAt(0));
		String[] atomTypeArray = {"H","N","C","O","S"};
		double[] atomWeightArray = {1.00794, 14.0067, 12.0107, 15.9994, 32.065};
			
		for(int i=0; i<atomTypeArray.length; ++i) {
			if (atomType.equals(atomTypeArray[i])) {
				atomWeight = atomWeightArray[i];
			}
		}
		
		return atomWeight;
	}
    
	public ArrayList<Residue> turnIntoResidueArray(ArrayList<Atom> atomList, 
		ArrayList<String> dsspFile, double bFactorMean, double bFactorSTD) {
		
		int residueAtoms, newResNum;
		double currentResidueBFactor, meanOfCurrentResidue, zScore, x, y, z;
		long countResidues = 0;
		boolean cTerm = false, nTerm = false;
		int currentResNum = atomList.get(0).getResNum();
		Residue curAtom = atomList.get(0), newAtom;
		ArrayList<Residue> resArray = new ArrayList<Residue>();
		ArrayList<Residue> tempArray = extractSS(dsspFile);
		ArrayList<Atom> currentlyInResidue = new ArrayList<Atom>();
		ArrayList<CartesianCoord> cartCoordOfAtomsOfResidueList = new ArrayList<CartesianCoord>();
		
		currentlyInResidue.add(curAtom);
		
		for (int i = 0; i<atomList.size(); ++i) {
			newAtom = atomList.get(i);
			newResNum = newAtom.getResNum();
			
			x = newAtom.getX();
			y = newAtom.getY();
			z = newAtom.getZ();
			cartCoordOfAtomsOfResidueList.add(new CartesianCoord(x,y,z));
			
			//if we're still on the same residue as before...
			if (newResNum == currentResNum) {
				++residueAtoms;
				currentResidueBFactor += newAtom.getBFactor();
				currentlyInResidue.add(newAtom);
				if(newAtom.getNTerm()) { nTerm = true; }
				if(newAtom.getCTerm()) { cTerm = true; }
			}

	   		//if we've moved on to the next residue
			else {
				meanOfCurrentResidue = currentResidueBFactor / residueAtoms;
				zScore = zScore(meanOfCurrentResidue, bFactorMean, bFactorSTD);
				
				//String pdbResNum, double bFactor, String ssType, 
				//CartesianCoord coords, boolean nTerm, boolean cTerm
				
				//if zscore is too high set as missing HERE
				if(zScore < ____________ ) { // zscore falls into parameters
					resArray.add(new Residue(currentResNum, zScore, "", 
					cartCoordOfAtomsOfResidueList, nTerm, cTerm, currentlyInResidue));
				}
				else {
					resArray.add(new Residue(currentResNum, false));
				}
				cartCoordOfAtomsOfResidueList.clear();
				currentlyInResidue.clear();
				currentResNum = newResNum;
				residueAtoms = 0;
				currentResidueBFactor = atomList.get(i).getBFactor();
				cTerm = atomList.get(i).getCTerm();
				nTerm = atomList.get(i).getNTerm();
			}
		}

		tempArray = sortResidues(tempArray);
		resArray = sortResidues(resArray);		
		ArrayList<Residue> finalResArray;
		
		if (Integer.parseInt(tempArray.get(0).getResNum()) < Integer.parseInt(resArray.get(0).getResNum())) {
			finalResArray = mergeArrays(tempArray, resArray, true);	
		}
		else {
			finalResArray = mergeArrays(resArray, tempArray, false);
		}
		
		ArrayList<Residue> pmoiArray = sortResidues(calculatePMOI(finalResArray, atomList));
	}
    
      
	public ArrayList<Residue> mergeArrays(ArrayList<Residue> lowerArray, ArrayList<Residue> higherArray, 
		boolean ssFirst) {
		
		// go through that one til they both match up, marking them as 'don't exist in both',
		int lCounter = 0;
		ArrayList<Residue> finalResArray = new ArrayList<Residue>();
		int finalCounter = 0;
		
		while (Integer.parseInt(lowerArray.get(lCounter).getResNum()) != 
			Integer.parseInt(higherArray.get(0).getResNum())) {
			
			String pdb = lowerArray.get(lCounter).getResNum();
			finalResArray.add(new Residue(pdb, false));
			++lCounter;
		} // end while

		//now we are at a point where the two arrays are synced, starting at lowerArray(0) and higherArray(counter)
		int hCounter = 0;
		int limit = Math.max(lowerArray.size(), higherArray.size());
		
		for (int j = 0; j < limit; ++j) {
			Residue currentLower = lowerArray.get(lCounter + j);
			Residue currentHigher = higherArray.get(hCounter + j);
			int lResNum = Integer.parseInt(currentLower.getResNum());
			int hResNum = Integer.parseInt(currentHigher.getResNum());
			
			while (lResNum < hResNum) {
				//mark ones that don't match as 'don't exist'...this may be more complicated than previously thought.
				finalResArray.add(new Residue("" + lResNum + "", false)); 
				++lCounter;
				currentLower = lowerArray.get(lCounter + j);
				lResNum = Integer.parseInt(currentLower.getResNum());
			} // end while 
	
			while (lResNum > hResNum) {
				finalResArray.add(new Residue("" + hResNum + "", false));
				++hCounter;
				currentHigher = higherArray.get(hCounter + j);
				hResNum = Integer.parseInt(currentHigher.getResNum());
			} // end other while
			
			while (lResNum == hResNum) {
				finalResArray.add(mergeResidues(currentLower, currentHigher, ssFirst));	
			} // end while
		} // end for
	} // end method
    
	public Residue mergeResidues(Residue res1, Residue res2, boolean ssFirst) {
		Residue retMe;
		
		if(!ssFirst) {
			Residue temp = res2;
			res2 = res1;
			res1 = temp;
		}
		//ss info is first
		String ss = res1.getSS();
		String pdb = res1.getResNum();
		double bF = res2.getBFactor();
		boolean cTerm = res2.getCTerm();
		boolean nTerm = res2.getNTerm();
		
		//String pdbResNum, double bFactor, String ssType, CartesianCoord coords, boolean nTerm, boolean cTerm
		retMe = new Residue(pdb, bF, ss, COORDS_NEEDED, nTerm, cTerm);
		
	}

	public ArrayList<Residue> extractSS(ArrayList<String> dsspFile) {
		ArrayList<Residue> tempArray = new ArrayList<Residue>();
		
		String[] sheetArray = {"E","B"};
		String[] helixArray = {"G","H","I"};
		String[] turnArray = {" ","S","T"};
		
		for (int i = 0; i<dsspFile.size(); ++i) {
		    if(charsAtEqual(dsspFile, i, 14, sheetArray)) {
				tempArray.get(i).setSSType("S");
		    }
		    else if(charsAtEqual(dsspFile, i, 14, helixArray)) {
				tempArray.get(i).setSSType("H");
		    }
		    else if(charsAtEqual(dsspFile, i, 14, turnArray)) {
				tempArray.get(i).setSSType("T");
		    }
		}	
		return tempArray;
	}

	public boolean charAtEquals(ArrayList<String> file, int index, int num, String charac) {
		String str = Character.toString(file.get(index).charAt(num));
		return str.equals(charac);
	}
    
	public boolean charsAtEqual(ArrayList<String> file, int index, int num, String[] chars) {
		for (int j=0; j<chars.length; ++j){
			if(!charAtEquals(file, index, num, chars[j])) {
				return false;
			} // end if   	
		} // end for
		return true;
	}
}