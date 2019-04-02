import java.io.*;
import java.util.HashMap;


public class Edl {
	
	// nombre max de modules, taille max d'un code objet d'une unite
	static final int MAXMOD = 5, MAXOBJ = 1000;
	// nombres max de references externes (REF) et de points d'entree (DEF)
	// pour une unite
	private static final int MAXREF = 10, MAXDEF = 10;
	
	// typologie des erreurs
	private static final int FATALE = 0, NONFATALE = 1;
	
	// valeurs possibles du vecteur de translation
	private static final int TRANSDON=1,TRANSCODE=2,REFEXT=3;
	
	// table de tous les descripteurs concernes par l'edl
	static Descripteur[] tabDesc = new Descripteur[MAXMOD + 1];
	
	// declarations de variables A COMPLETER SI BESOIN
	static int ipo, nMod, nbErr;
	static String nomProg;
	static String[] nomModule = new String[5];

	// utilitaire de traitement des erreurs
	// ------------------------------------
	static void erreur(int te, String m) {
		System.out.println(m);
		if (te == FATALE) {
			System.out.println("ABANDON DE L'EDITION DE LIENS");
			System.exit(1);
		}
		nbErr = nbErr + 1;
	}

	// utilitaire de remplissage de la table des descripteurs tabDesc
	// --------------------------------------------------------------
	static void lireDescripteurs() {
		String s;
		System.out.println("les noms doivent etre fournis sans suffixe");
		System.out.print("nom du programme : ");
		s = Lecture.lireString();
		tabDesc[0] = new Descripteur();
		tabDesc[0].lireDesc(s);
		if (!tabDesc[0].getUnite().equals("programme"))
			erreur(FATALE, "programme attendu");
		nomProg = s;
		
		nMod = 0;
		while (!s.equals("") && nMod < MAXMOD) {
			System.out.print("nom de module " + (nMod + 1)
					+ " (RC si termine) ");
			s = Lecture.lireString();
			if (!s.equals("")) {
				nMod = nMod + 1;
				tabDesc[nMod] = new Descripteur();
				tabDesc[nMod].lireDesc(s);
				nomModule[nMod-1] = s;
				if (!tabDesc[nMod].getUnite().equals("module"))
					erreur(FATALE, "module attendu");
			}
		}
	}

	
	static void constMap() {
		// f2 = fichier executable .map construit
		OutputStream f2 = Ecriture.ouvrir(nomProg + ".map");
		if (f2 == null)
			erreur(FATALE, "creation du fichier " + nomProg
					+ ".map impossible");
		// pour construire le code concatene de toutes les unit�s
		int[] po = new int[(nMod + 1) * MAXOBJ + 1];
		
		int[][] trans = new int[tabDesc.length][2];
		HashMap<String, int[]> dicoDef = new HashMap<String, int[]>();
		int[][] adFinale = new int[6][11];
		
		int nbReserver = 0;
		
		for(int i = 0; i < nMod+1; i++) {
			
			nbReserver += tabDesc[i].getTailleGlobaux();
			
			if(i == 0) {
				trans[0][0] = 0;
				trans[0][1] = 0;
			} else {
				trans[i][0] = trans[i-1][0] + tabDesc[i-1].getTailleGlobaux();
				trans[i][1] = trans[i-1][1] + tabDesc[i-1].getTailleCode();
			}
			
			for(int j = 1; j < tabDesc[i].getNbDef() + 1; j++) {
				int[] tmp = new int[2];
				tmp[0] = tabDesc[i].getDefAdPo(j) + trans[i][1];
				tmp[1] = tabDesc[i].getDefNbParam(j);
				
				if(dicoDef.size() < 61 && dicoDef.get(tabDesc[i].getDefNomProc(j)) == null) {
					dicoDef.put(tabDesc[i].getDefNomProc(j), tmp);
				} else {
					erreur(FATALE, "Plusieurs fois meme Def ou trop de Def");
				}
			}
		
		}
		
		for(int i = 0; i < nMod+1; i++) {
			for(int k = 1; k < tabDesc[i].getNbRef() + 1; k++) {
				adFinale[i][k-1] = dicoDef.get(tabDesc[i].getRefNomProc(k))[0];
			}
		}
		
		int nbTransExt = tabDesc[0].getNbTransExt(); 
		
		InputStream obj = Lecture.ouvrir(nomProg +".obj");
		
		HashMap<Integer, Integer> transExt = new HashMap<Integer, Integer>(); 
		
		while(nbTransExt > 0) {
			transExt.put(Lecture.lireInt(obj),  Lecture.lireInt(obj));
			nbTransExt--;
		}
		
		
		po[1] = 1;
		Ecriture.ecrireInt(f2, po[1]);
		Ecriture.ecrireChar(f2, '\n');
		po[2] = nbReserver;
		Ecriture.ecrireInt(f2, po[2]);
		Ecriture.ecrireChar(f2, '\n');
			
		Lecture.lireInt(obj);
		Lecture.lireInt(obj); 
			
		
		
		ipo = 2;
		
		while(!Lecture.finFichier(obj)){
			ipo++;
			if(transExt.get(ipo) == null) {
				po[ipo] = Lecture.lireInt(obj);
			} else {
				switch(transExt.get(ipo)) {
					case TRANSDON:
						po[ipo] = Lecture.lireInt(obj) + trans[0][0];
						break;
					case TRANSCODE:
						po[ipo] = Lecture.lireInt(obj) + trans[0][1];
						break;
					case REFEXT:
						po[ipo] = adFinale[0][Lecture.lireInt(obj)-1];
						break;
				}
				
			}
			
			Ecriture.ecrireInt(f2, po[ipo]);
			Ecriture.ecrireChar(f2, '\n');
		}

		Lecture.fermer(obj);
		
		for(int i = 0; i < nMod; i++) {
			
			nbTransExt = tabDesc[i+1].getNbTransExt(); 
			
			obj = Lecture.ouvrir(nomModule[i] +".obj");
		
			transExt = new HashMap<Integer, Integer>(); 
		
			while(nbTransExt > 0) {
				transExt.put(Lecture.lireInt(obj),  Lecture.lireInt(obj));
				nbTransExt--;
			}
			
			int indexMod = 1;
		
			while(!Lecture.finFichier(obj)){
				ipo++;
				if(transExt.get(indexMod) == null) {
					po[ipo] = Lecture.lireInt(obj);
				
				} else {
					
					switch(transExt.get(indexMod)) {
						case TRANSDON:
							po[ipo] = Lecture.lireInt(obj) + trans[i+1][0];
							break;
						case TRANSCODE:
							po[ipo] = Lecture.lireInt(obj) + trans[i+1][1];
							break;
						case REFEXT:
							po[ipo] = adFinale[i+1][Lecture.lireInt(obj)-1];
							break;
					}
					
					
				}
				indexMod++;
				
				Ecriture.ecrireInt(f2, po[ipo]);
				Ecriture.ecrireChar(f2, '\n');
			}
			Lecture.fermer(obj);
		}

		Ecriture.fermer(f2);
		
		// creation du fichier en mnemonique correspondant
		Mnemo.creerFichier(ipo, po, nomProg + ".ima");
	}

	public static void main(String argv[]) {
		System.out.println("EDITEUR DE LIENS / PROJET LICENCE");
		System.out.println("---------------------------------");
		System.out.println("");
		nbErr = 0;
		
		// Phase 1 de l'edition de liens
		// -----------------------------
		lireDescripteurs();		// lecture des descripteurs a completer si besoin
// 
// ... A COMPLETER ...
//
		if (nbErr > 0) {
			System.out.println("programme executable non produit");
			System.exit(1);
		}
		
		// Phase 2 de l'edition de liens
		// -----------------------------
		constMap();				// a completer
		System.out.println("Edition de liens terminee");
	}
}
