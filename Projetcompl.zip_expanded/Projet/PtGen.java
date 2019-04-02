
// RONCIER_LABBE_LONGRAIS

/*********************************************************************************
 * VARIABLES ET METHODES FOURNIES PAR LA CLASSE UtilLex (cf libclass)            *
 *       complement à l'ANALYSEUR LEXICAL produit par ANTLR                      *
 *                                                                               *
 *                                                                               *
 *   nom du programme compile, sans suffixe : String UtilLex.nomSource           *
 *   ------------------------                                                    *
 *                                                                               *
 *   attributs lexicaux (selon items figurant dans la grammaire):                *
 *   ------------------                                                          *
 *     int UtilLex.valNb = valeur du dernier nombre entier lu (item nbentier)    *
 *     int UtilLex.numId = code du dernier identificateur lu (item ident)        *
 *                                                                               *
 *                                                                               *
 *   methodes utiles :                                                           *
 *   ---------------                                                             *
 *     void UtilLex.messErr(String m)  affichage de m et arret compilation       *
 *     String UtilLex.repId(int nId) delivre l'ident de codage nId               *
 *     void afftabSymb()  affiche la table des symboles                          *
 *********************************************************************************/

import java.io.*;

import javax.lang.model.util.ElementScanner6;

// classe de mise en oeuvre du compilateur
// =======================================
// (verifications semantiques + production du code objet)

public class PtGen {
	static int affect = 0;
	static int categorieVar = 0;
	// constantes manipulees par le compilateur
	// ----------------------------------------

	private static final int

	// taille max de la table des symboles
	MAXSYMB = 300,

			// codes MAPILE :
			RESERVER = 1, EMPILER = 2, CONTENUG = 3, AFFECTERG = 4, OU = 5, ET = 6, NON = 7, INF = 8, INFEG = 9,
			SUP = 10, SUPEG = 11, EG = 12, DIFF = 13, ADD = 14, SOUS = 15, MUL = 16, DIV = 17, BSIFAUX = 18,
			BINCOND = 19, LIRENT = 20, LIREBOOL = 21, ECRENT = 22, ECRBOOL = 23, ARRET = 24, EMPILERADG = 25,
			EMPILERADL = 26, CONTENUL = 27, AFFECTERL = 28, APPEL = 29, RETOUR = 30,

			// codes des valeurs vrai/faux
			VRAI = 1, FAUX = 0,

			// types permis :
			ENT = 1, BOOL = 2, NEUTRE = 3,

			// cat�gories possibles des identificateurs :
			CONSTANTE = 1, VARGLOBALE = 2, VARLOCALE = 3, PARAMFIXE = 4, PARAMMOD = 5, PROC = 6, DEF = 7, REF = 8,
			PRIVEE = 9,

			// valeurs possible du vecteur de translation
			TRANSDON = 1, TRANSCODE = 2, REFEXT = 3;

	// utilitaires de controle de type
	// -------------------------------

	private static void verifEnt() {
		if (tCour != ENT)
			UtilLex.messErr("expression entiere attendue");
	}

	private static void verifBool() {
		if (tCour != BOOL)
			UtilLex.messErr("expression booleenne attendue");
	}

	// pile pour gerer les chaines de reprise et les branchements en avant
	// -------------------------------------------------------------------

	private static TPileRep pileRep;

	// production du code objet en memoire
	// -----------------------------------

	private static ProgObjet po;

	// COMPILATION SEPAREE
	// -------------------
	//
	// modification du vecteur de translation associe au code produit
	// + incrementation attribut nbTransExt du descripteur
	// NB: effectue uniquement si c'est une reference externe ou si on compile un
	// module
	private static void modifVecteurTrans(int valeur) {
		if (valeur == REFEXT || desc.getUnite().equals("module")) {
			po.vecteurTrans(valeur);
			desc.incrNbTansExt();
		}
	}

	// descripteur associe a un programme objet
	private static Descripteur desc;

	// autres variables fournies
	// -------------------------
	public static String trinome = "RoncierLabbeLongrais"; // MERCI de renseigner ici un nom pour le trinome, constitue
															// de exclusivement de lettres

	private static int tCour; // type de l'expression compilee
	private static int vCour; // valeur de l'expression compilee le cas echeant

	// D�finition de la table des symboles
	//
	private static EltTabSymb[] tabSymb = new EltTabSymb[MAXSYMB + 1];

	// it = indice de remplissage de tabSymb
	// bc = bloc courant (=1 si le bloc courant est le programme principal)
	private static int it, bc;

	// utilitaire de recherche de l'ident courant (ayant pour code UtilLex.numId)
	// dans tabSymb
	// rend en resultat l'indice de cet ident dans tabSymb (O si absence)
	private static int presentIdent(int binf) {
		int i = it;
		while (i >= binf && tabSymb[i].code != UtilLex.numId)
			i--;
		if (i >= binf)
			return i;
		else
			return 0;
	}

	// utilitaire de placement des caracteristiques d'un nouvel ident dans tabSymb
	//
	private static void placeIdent(int c, int cat, int t, int v) {
		if (it == MAXSYMB)
			UtilLex.messErr("debordement de la table des symboles");
		it = it + 1;
		tabSymb[it] = new EltTabSymb(c, cat, t, v);
	}

	// utilitaire d'affichage de la table des symboles
	//
	private static void afftabSymb() {
		System.out.println("       code           categorie      type    info");
		System.out.println("      |--------------|--------------|-------|----");
		for (int i = 1; i <= it; i++) {
			if (i == bc) {
				System.out.print("bc=");
				Ecriture.ecrireInt(i, 3);
			} else if (i == it) {
				System.out.print("it=");
				Ecriture.ecrireInt(i, 3);
			} else
				Ecriture.ecrireInt(i, 6);
			if (tabSymb[i] == null)
				System.out.println(" reference NULL");
			else
				System.out.println(" " + tabSymb[i]);
		}
		System.out.println();
	}

	// initialisations A COMPLETER SI BESOIN
	// -------------------------------------

	public static void initialisations() {

		// indices de gestion de la table des symboles
		it = 0;
		bc = 1;

		// pile des reprises pour compilation des branchements en avant
		pileRep = new TPileRep();
		// programme objet = code Mapile de l'unite en cours de compilation
		po = new ProgObjet();
		// COMPILATION SEPAREE: desripteur de l'unite en cours de compilation
		desc = new Descripteur();

		// initialisation necessaire aux attributs lexicaux
		UtilLex.initialisation();

		// initialisation du type de l'expression courante
		tCour = NEUTRE;

	} // initialisations

	// code des points de generation A COMPLETER
	// -----------------------------------------
	public static void pt(int numGen) {

		
		switch (numGen) {
		case 0:
			initialisations();
			break;

		/**** Declaration ****/

		case 1: // recuperation d'un entier positif
			tCour = ENT;
			vCour = UtilLex.valNb;

			break;
		case 2: // recuperation d'un entier negatif
			tCour = ENT;
			vCour = -(UtilLex.valNb);
			break;
		case 3: // recuperation d'un boolean vrai
			tCour = BOOL;
			vCour = 1;

			break;
		case 4:// recuperation d'un boolean faux
			tCour = BOOL;
			vCour = 0;
			break;
		case 5:// Verification presence constante
			if (presentIdent(bc) > 0) {
				UtilLex.messErr("Constante deja declaree ptgen 5");
			}
			break;
		case 6:// Ajout constante dans tabSymb
			if (it < MAXSYMB) {
				placeIdent(UtilLex.numId, CONSTANTE, tCour, vCour);
			} else {
				UtilLex.messErr("Trop de symbole ptgen 6");
			}

			break;
		case 7:// type courant = ENT
			tCour = ENT;
			break;
		case 8:// type courant = BOOL
			tCour = BOOL;
			break;
		case 9:// Ajout de la var dans tabSymb
				// Doit-on verifier qu'elle existe deja ? /!\/!\
			if (presentIdent(bc) > 0) {
				UtilLex.messErr("Ident deja declaree ptgen 9");
			}
			if (it < MAXSYMB) {
				int addrVar;
				if (it == 0 || (tabSymb[it].categorie == CONSTANTE && categorieVar != VARLOCALE) || tabSymb[it].categorie == PRIVEE) {
					addrVar = 0;
				}
				else if(tabSymb[it].categorie == PARAMFIXE || tabSymb[it].categorie == PARAMMOD ||tabSymb[it].categorie == CONSTANTE) {
					addrVar = tabSymb[bc - 1].info + 2;
				} else {
					addrVar = tabSymb[it].info + 1;
				}
				placeIdent(UtilLex.numId, categorieVar, tCour, addrVar);
			} else {
				UtilLex.messErr("Trop de symbole dans TabSymb ptgen 9");
			}
			break;
		case 10:// Production du code MAPILE pour la creation des var
			if (tabSymb[it].categorie == VARGLOBALE) {
				int nbVar = tabSymb[it].info + 1;
				if(desc.getUnite().equals("programme")) {
					po.produire(RESERVER);
					po.produire(nbVar);
				}
				desc.setTailleGlobaux(nbVar);
			} else if(tabSymb[it].categorie == VARLOCALE) {
				int nbVar = tabSymb[it].info - (tabSymb[bc-1].info + 1);
				po.produire(RESERVER);
				po.produire(nbVar);
			} else {
				UtilLex.messErr("Pas de variable dans tabSymb ptgen 10");
			}
			break;
		case 11:// Me souviens plus de ce qu'il est sensé faire
			break;
		case 12:// Sauvegarde de la categorie de la variable
			categorieVar = VARGLOBALE;
			break;

		/**** Expression ****/

		case 20:// Recuperation et ajout numId dans la pile [PROC OK]
			int posIdent = presentIdent(1);
			if (posIdent > 0) {
				if (tabSymb[posIdent].categorie == CONSTANTE) {
					tCour = tabSymb[posIdent].type;
					po.produire(EMPILER);
					po.produire(tabSymb[posIdent].info);
				} else if (tabSymb[posIdent].categorie == VARGLOBALE) {
					tCour = tabSymb[posIdent].type;
					po.produire(CONTENUG);
					po.produire(tabSymb[posIdent].info);
					modifVecteurTrans(TRANSDON);
				}else if(tabSymb[posIdent].categorie == VARLOCALE || tabSymb[posIdent].categorie == PARAMFIXE) {
					tCour = tabSymb[posIdent].type;
					po.produire(CONTENUL);
					po.produire(tabSymb[posIdent].info);
					po.produire(0);
				}else if(tabSymb[posIdent].categorie == PARAMMOD) {
					tCour = tabSymb[posIdent].type;
					po.produire(CONTENUL);
					po.produire(tabSymb[posIdent].info);
					po.produire(1);
				} else {
					UtilLex.messErr("Ident n'est ni une variable, ni une constante ptgen 20");
				}
			} else {
				UtilLex.messErr("Variable ou constante inexistante ptgen 20");
			}
			break;
		case 21:// verification tCour = BOOL [PROC OK]
			verifBool();
			break;
		case 22:// verification tCour = BOOL + produire OU [PROC OK]
			verifBool();
			po.produire(OU);
			break;
		case 23:// verification tCour = BOOL + produire ET [PROC OK]
			verifBool();
			po.produire(ET);
			break;
		case 24:// verification tCour = BOOL + produire NON [PROC OK]
			verifBool();
			po.produire(NON);
			break;
		case 25:// verification tCour = ENT [PROC OK]
			verifEnt();
			break;
		case 26:// verification tCour = ENT + produire = + tCour = BOOL [PROC OK]
			verifEnt();
			po.produire(EG);
			tCour = BOOL;
			break;
		case 27:// verification tCour = ENT + produire <> + tCour = BOOL [PROC OK]
			verifEnt();
			po.produire(DIFF);
			tCour = BOOL;
			break;
		case 28:// verification tCour = ENT + produire > + tCour = BOOL [PROC OK]
			verifEnt();
			po.produire(SUP);
			tCour = BOOL;
			break;
		case 29:// verification tCour = ENT + produire >= + tCour = BOOL [PROC OK]
			verifEnt();
			po.produire(SUPEG);
			tCour = BOOL;
			break;
		case 30:// verification tCour = ENT + produire < + tCour = BOOL [PROC OK]
			verifEnt();
			po.produire(INF);
			tCour = BOOL;
			break;
		case 31:// verification tCour = ENT + produire <= + tCour = BOOL [PROC OK]
			verifEnt();
			po.produire(INFEG);
			tCour = BOOL;
			break;
		case 32:// verification tCour = ENT + produire ADD [PROC OK] 
			verifEnt();
			po.produire(ADD);
			break;
		case 33:// verification tCour = ENT + produire SOUS [PROC OK]
			verifEnt();
			po.produire(SOUS);
			break;
		case 34:// verification tCour = ENT + produire MUL [PROC OK]
			verifEnt();
			po.produire(MUL);
			break;
		case 35:// verification tCour = ENT + produire DIV [PROC OK]
			verifEnt();
			po.produire(DIV);
			break;
		case 36:// Ajout dans la pile de vCour [PROC OK]
			po.produire(EMPILER); 
			po.produire(vCour);
			break;
			
			/**** Ecriture / Lecture / Affectation ****/
			
		case 40:// Ecriture [PROC OK]
			if (tCour == BOOL) {
				po.produire(ECRBOOL);
			} else if (tCour == ENT) {
				po.produire(ECRENT);
			} else {
				UtilLex.messErr("Ecriture impossible car pas bool ni ent ptgen 40");
			}
			break;
		case 41:// Lecture [PROC OK]
			int tmp = presentIdent(bc);
			if (tmp > 0 && tabSymb[tmp].type == ENT) {
				po.produire(LIRENT);
			} else if (tmp > 0 && tabSymb[tmp].type == BOOL) {
				po.produire(LIREBOOL);
			} else {
				UtilLex.messErr("Lecture impossible, var inexistante ptgen 41");
			}
			
			if(tabSymb[tmp].categorie == VARGLOBALE) {
				po.produire(AFFECTERG);
				po.produire(tabSymb[tmp].info);
				modifVecteurTrans(TRANSDON);
			}else if(tabSymb[tmp].categorie == VARLOCALE) {
				po.produire(AFFECTERL);
				po.produire(tabSymb[tmp].info);
				po.produire(0);
			}else if(tabSymb[tmp].categorie == PARAMMOD) {
				po.produire(AFFECTERL);
				po.produire(tabSymb[tmp].info);
				po.produire(1);
			}else {
				UtilLex.messErr("mauvais type var ptgen 41");
			}

			break;
		case 50: // Affect [PROC OK]
			tmp = affect; 
			if (tmp > 0 && tabSymb[tmp].categorie == VARGLOBALE
					&& ((tabSymb[tmp].type == ENT && tCour == ENT) || (tabSymb[tmp].type == BOOL && tCour == BOOL))) {
				po.produire(AFFECTERG);
				po.produire(tabSymb[tmp].info);
				modifVecteurTrans(TRANSDON);
			} else if(tmp > 0 && tabSymb[tmp].categorie == VARLOCALE
					&& ((tabSymb[tmp].type == ENT && tCour == ENT) || (tabSymb[tmp].type == BOOL && tCour == BOOL))){
				po.produire(AFFECTERL);
				po.produire(tabSymb[tmp].info);
				po.produire(0);
			} else if(tmp > 0 && tabSymb[tmp].categorie == PARAMMOD
					&& ((tabSymb[tmp].type == ENT && tCour == ENT) || (tabSymb[tmp].type == BOOL && tCour == BOOL))){
				po.produire(AFFECTERL);
				po.produire(tabSymb[tmp].info);
				po.produire(1);
			} else if (tmp > 0 ){
				UtilLex.messErr(UtilLex.repId(tabSymb[tmp].code) + " : Mauvais type ptgen 50");
			}else {
				UtilLex.messErr("var inexistante ptgen 50");
			}
			break;
		case 51: // Sauvegarde var affect dest [PROC OK]
			affect = presentIdent(1);
			break;
			
		/**** Conditions ****/

			/**** si ****/
			
		case 100: // Verification expression bool + production MAPILE "si" + preparation pile reprise [PROC OK]
			verifBool();
			po.produire(BSIFAUX);
			po.produire(-1);
			modifVecteurTrans(TRANSCODE);
			pileRep.empiler(po.getIpo());
			break;
		case 101: // production code MAPILE "sinon" [PROC OK]
			po.produire(BINCOND);
			po.produire(-1);
			modifVecteurTrans(TRANSCODE);
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			pileRep.empiler(po.getIpo());
			break;
		case 102: // depilement pile reprise + mise a jour code MAPILE [PROC OK]
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			break;
			
			/**** ttq ****/
			
		case 110: // preparation pile reprise [PROC OK]
			pileRep.empiler(po.getIpo()+1);
			break;
		case 111: // Verification expression bool + production MAPILE "ttq" + actualisation pile reprise [PROC OK]
			verifBool();
			po.produire(BSIFAUX);
			po.produire(-1);
			modifVecteurTrans(TRANSCODE);
			pileRep.empiler(po.getIpo());
			break;
		case 112: // depilement pile reprise + mise a jour code MAPILE [PROC OK]
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			modifVecteurTrans(TRANSCODE);
			po.produire(pileRep.depiler());
			break;
		
			/**** cond ****/
		
		case 120: // preparation pile reprise avec 0 [PROC OK]
			pileRep.empiler(0);
			break;
		case 121: // verif bool + mise a jour pile reprise + production MAPILE "cond" [PROC OK]
			verifBool();
			po.produire(BSIFAUX);
			modifVecteurTrans(TRANSCODE);
			po.produire(-1);
			pileRep.empiler(po.getIpo());
			break;
		case 122: // actualisation pile reprise + mise a jour code MAPILE [PROC OK]
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			modifVecteurTrans(TRANSCODE);
			po.produire(pileRep.depiler());
			pileRep.empiler(po.getIpo());
			break;
		case 123: // actualisation pile reprise + mise a jour code MAPILE pour"aut" [PROC OK]
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			modifVecteurTrans(TRANSCODE);
			po.produire(pileRep.depiler());
			pileRep.empiler(po.getIpo());
			break;
		case 124: // actualisation pile reprise popur "aut" [PROC OK]
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			break;
		case 125: // mise a jour pile reprise [PROC OK]
			int bincond = po.getIpo()+1;
			int elt = pileRep.depiler();
			
			while(elt != 0) {
				tmp = po.getElt(elt);
				po.modifier(elt, bincond);
				elt = tmp;
			}
			break;
			
		/**** Proc ****/
			
			/**** Decl ****/
			
		case 200: // creation BINCOND avant procs + pileRep
			if(desc.getUnite().equals("programme")) {
				po.produire(BINCOND);
				po.produire(-1);
				modifVecteurTrans(TRANSCODE);
				pileRep.empiler(po.getIpo());
			}
			break;
		case 201: // Actualisation BINCOND avant procs + pileRep
			if(desc.getUnite().equals("programme")) {
				po.modifier(pileRep.depiler(), po.getIpo()+1);
			}
			break;
		case 202: // ajout de la proc dans tabSymb
			if(presentIdent(bc) > 0){
				UtilLex.messErr("proc deja declaree ptgen 202");
			}
			placeIdent(UtilLex.numId, PROC, NEUTRE, po.getIpo()+1);
			placeIdent(-1, PRIVEE, NEUTRE, -1);
			bc = it+1;
			break;
		case 203: // ajout du paramfixe dans tabSymb
			if (presentIdent(bc) > 0) {
				UtilLex.messErr("paramfixe deja declare ptgen 203");
			}
			if (it < MAXSYMB) {
				int addrVar = -1;
				if (tabSymb[it].categorie == PRIVEE) {
					addrVar = 0;
				} else if(tabSymb[it].categorie == PARAMFIXE) {
					addrVar = tabSymb[it].info + 1;
				} else {
					UtilLex.messErr("Erreur ajout parametre fixe ptgen 203");
				}
				placeIdent(UtilLex.numId, PARAMFIXE, tCour, addrVar);
				
			} else {
				UtilLex.messErr("Trop de symbole dans TabSymb ptgen 203");
			}
			break;
		case 204: // ajout du parammod dans tabSymb
			if (presentIdent(bc) > 0) {
				UtilLex.messErr("parammod deja declare ptgen 204");
			}
			if (it < MAXSYMB) {
				int addrVar = -1;
				if (tabSymb[it].categorie == PRIVEE) {
					addrVar = 0;
				} else if(tabSymb[it].categorie == PARAMMOD || tabSymb[it].categorie == PARAMFIXE) {
					addrVar = tabSymb[it].info + 1;
				} else {
					UtilLex.messErr("Erreur ajout parametre mod ptgen 204");
				}
				placeIdent(UtilLex.numId, PARAMMOD, tCour, addrVar);
			} else {
				UtilLex.messErr("Trop de symbole dans TabSymb ptgen 204");
			}
			break;
		case 207: // Actualisation du nombre de parametres de la proc + actualisation tabDef 
			tabSymb[bc-1].info = it - bc + 1;
			if(desc.getUnite().equals("module")) {
				tmp = desc.presentDef(UtilLex.repId(tabSymb[bc-2].code));
				if(tmp > 0) {
					desc.modifDefNbParam(tmp, tabSymb[bc-1].info);
					desc.modifDefAdPo(tmp, tabSymb[bc-2].info);
				} else {
					UtilLex.messErr("Def inexistante ptgen 207");
				}
			}
			break;
		case 208:// Sauvegarde de la categorie de la variable
			categorieVar = VARLOCALE;
			break;
		case 209:// gestion du retour de la proc
			po.produire(RETOUR);
			po.produire(tabSymb[bc-1].info);
			it = (bc + tabSymb[bc-1].info - 1);
			for(int i = bc; i <= it; i++) {
				tabSymb[i].code = -1;
			}
			bc = 1;
			break;
			
			/**** Appel ****/
			
		case 220: // appel proc
			tmp = affect;
			if(tmp > 0 && tabSymb[tmp].categorie == PROC){
				po.produire(APPEL);
				po.produire(tabSymb[tmp].info);
				modifVecteurTrans(REFEXT);
				po.produire(tabSymb[tmp+1].info);
			}else{
				UtilLex.messErr("pas proc ptgen 220");
			}
			break;
		case 221: // ajout dans la pile d'un parametre mod 
			tmp = presentIdent(1);
			if(tabSymb[tmp].categorie == VARGLOBALE) {
				po.produire(EMPILERADG);
				po.produire(tabSymb[tmp].info);
				modifVecteurTrans(TRANSDON);
			}else if (tabSymb[tmp].categorie == VARLOCALE) {
				po.produire(EMPILERADL);
				po.produire(tabSymb[tmp].info);
				po.produire(0);
			}else if (tabSymb[tmp].categorie == PARAMMOD) {
				po.produire(EMPILERADL);
				po.produire(tabSymb[tmp].info);
				po.produire(1);
			}else {
				UtilLex.messErr("Mauvais type de var ptgen 221");
			}
			break;
		
		/**** Reference ****/
		
		case 300: // ajout de la ref dans tabRef et dans tabSymb
			desc.ajoutRef(UtilLex.repId(UtilLex.numId));
			desc.modifRefNbParam(desc.getNbRef(), 0);

			if(presentIdent(bc) > 0){
				UtilLex.messErr("proc deja declaree ptgen 202");
			}
			placeIdent(UtilLex.numId, PROC, NEUTRE, desc.getNbRef());
			placeIdent(-1, PRIVEE, NEUTRE, 0);
			break;
			
		case 301: // actualisation nbParam fixe
			desc.modifRefNbParam(desc.getNbRef(), desc.getRefNbParam(desc.getNbRef())+1);

			tmp = presentIdent(1);

			if(tmp > 0){
				tabSymb[tmp+1].info = tabSymb[tmp+1].info + 1;
			}else{
				UtilLex.messErr("proc inexistante ptgen 301");
			}

			if (it < MAXSYMB) {
				int addrVar = -1;
				if (tabSymb[it].categorie == PRIVEE) {
					addrVar = 0;
				} else if(tabSymb[it].categorie == PARAMFIXE) {
					addrVar = tabSymb[it].info + 1;
				} else {
					UtilLex.messErr("Erreur ajout parametre fixe ptgen 301");
				}
				placeIdent(-1, PARAMFIXE, tCour, addrVar);
				
			} else {
				UtilLex.messErr("Trop de symbole dans TabSymb ptgen 301");
			}

			break;

		case 302: // actualisation nbParam mod
			desc.modifRefNbParam(desc.getNbRef(), desc.getRefNbParam(desc.getNbRef())+1);

			tmp = presentIdent(1);

			if(tmp > 0){
				tabSymb[tmp+1].info = tabSymb[tmp+1].info + 1;
			}else{
				UtilLex.messErr("proc inexistante ptgen 301");
			}

			if (it < MAXSYMB) {
				int addrVar = -1;
				if (tabSymb[it].categorie == PRIVEE) {
					addrVar = 0;
				} else if(tabSymb[it].categorie == PARAMMOD || tabSymb[it].categorie == PARAMFIXE) {
					addrVar = tabSymb[it].info + 1;
				} else {
					UtilLex.messErr("Erreur ajout parametre mod ptgen 302");
				}
				placeIdent(-1, PARAMMOD, tCour, addrVar);
			} else {
				UtilLex.messErr("Trop de symbole dans TabSymb ptgen 302");
			}
			break;
		case 303: // actualisation unit� programme
			desc.setUnite("programme");
			break;
			
		case 304: // actualisation unit� module
			desc.setUnite("module");			
			break;
		case 305: // actualisation taille code
			desc.setTailleCode(po.getIpo());
			desc.ecrireDesc(UtilLex.nomSource);
			break;
		case 306: // ajout de la def dans tabRef
			desc.ajoutDef(UtilLex.repId(UtilLex.numId));
			break;
			
			
			
			
			
		/**** Arret ****/
			
		case 999:// Prduction arret
			if(desc.getUnite().equals("programme")) {
				po.produire(ARRET);
			}else {
				UtilLex.messErr("Erreur arret ptgen 999");
			}
			//afftabSymb();
			//po.constObj();
			break;
		
		
		// A COMPLETER

		default:
			System.out.println("Point de generation non prevu dans votre liste");
			break;

		}
		po.constObj();
		//afftabSymb();
		po.constGen();

	}
}
