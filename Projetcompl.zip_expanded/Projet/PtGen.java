
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

// classe de mise en oeuvre du compilateur
// =======================================
// (verifications semantiques + production du code objet)

public class PtGen {
	static int affect = 0;
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
				System.out.println(" r�f�rence NULL");
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
			if (presentIdent(1) > 0) {
				UtilLex.messErr("Constante deja declaree");
			}
			break;
		case 6:// Ajout constante dans tabSymb
			if (it < MAXSYMB) {
				placeIdent(UtilLex.numId, CONSTANTE, tCour, vCour);
			} else {
				UtilLex.messErr("Trop de symbole");
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
			if (presentIdent(1) > 0) {
				UtilLex.messErr("deja declaree");
			}
			if (it < MAXSYMB) {
				int addrVar;
				if (tabSymb[it].categorie == CONSTANTE) {
					addrVar = 0;
				} else {
					addrVar = tabSymb[it].info + 1;
				}
				placeIdent(UtilLex.numId, VARGLOBALE, tCour, addrVar);
			} else {
				UtilLex.messErr("Trop de symbole");
			}
			break;
		case 10:// Production du code MAPILE pour la creation des var
			if (tabSymb[it].categorie == VARGLOBALE) {
				int nbVar = tabSymb[it].info + 1;
				po.produire(RESERVER);
				po.produire(nbVar);
			} else {
				UtilLex.messErr("Pas de variable dans tabSymb");
			}
			break;
		case 11:// Me souviens plus de ce qu'il est sensé faire
			break;

		/**** Expression ****/

		case 20:// Recuperation et ajout numId dans la pile
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
				} else {
					UtilLex.messErr("Ident n'est ni une variable, ni une constante");
				}
			} else {
				UtilLex.messErr("Variable ou constante inexistante");
			}
			break;
		case 21:// verification tCour = BOOL
			verifBool();
			break;
		case 22:// verification tCour = BOOL + produire OU
			verifBool();
			po.produire(OU);
			break;
		case 23:// verification tCour = BOOL + produire ET
			verifBool();
			po.produire(ET);
			break;
		case 24:// verification tCour = BOOL + produire NON
			verifBool();
			po.produire(NON);
			break;
		case 25:// verification tCour = ENT
			verifEnt();
			break;
		case 26:// verification tCour = ENT + produire = + tCour = BOOL
			verifEnt();
			po.produire(EG);
			tCour = BOOL;
			break;
		case 27:// verification tCour = ENT + produire <> + tCour = BOOL
			verifEnt();
			po.produire(DIFF);
			tCour = BOOL;
			break;
		case 28:// verification tCour = ENT + produire > + tCour = BOOL
			verifEnt();
			po.produire(SUP);
			tCour = BOOL;
			break;
		case 29:// verification tCour = ENT + produire >= + tCour = BOOL
			verifEnt();
			po.produire(SUPEG);
			tCour = BOOL;
			break;
		case 30:// verification tCour = ENT + produire < + tCour = BOOL
			verifEnt();
			po.produire(INF);
			tCour = BOOL;
			break;
		case 31:// verification tCour = ENT + produire <= + tCour = BOOL
			verifEnt();
			po.produire(INFEG);
			tCour = BOOL;
			break;
		case 32:// verification tCour = ENT + produire ADD
			verifEnt();
			po.produire(ADD);
			break;
		case 33:// verification tCour = ENT + produire SOUS
			verifEnt();
			po.produire(SOUS);
			break;
		case 34:// verification tCour = ENT + produire MUL
			verifEnt();
			po.produire(MUL);
			break;
		case 35:// verification tCour = ENT + produire DIV
			verifEnt();
			po.produire(DIV);
			break;
		case 36:// Ajout dans la pile de vCour
			po.produire(EMPILER);
			po.produire(vCour);
			break;
			
			/**** Ecriture / Lecture / Affectation ****/
			
		case 40:// Ecriture
			if (tCour == BOOL) {
				po.produire(ECRBOOL);
			} else if (tCour == ENT) {
				po.produire(ECRENT);
			} else {
				UtilLex.messErr("Ecriture impossible car pas bool ni ent ptgen40");
			}
			break;
		case 41:// Lecture
			int tmp = presentIdent(1);
			if (tmp > 0 && tabSymb[tmp].type == ENT) {
				po.produire(LIRENT);
				po.produire(AFFECTERG);
				po.produire(tabSymb[tmp].info);
			} else if (tmp > 0 && tabSymb[tmp].type == BOOL) {
				po.produire(LIREBOOL);
				po.produire(AFFECTERG);
				po.produire(tabSymb[tmp].info);
			} else {
				UtilLex.messErr("Lecture impossible, var inexistante ptgen41");
			}

			break;
		case 50: // Affect
			tmp = affect;//presentIdent(1);
			if (tmp > 0 && tabSymb[tmp].categorie == VARGLOBALE
					&& ((tabSymb[tmp].type == ENT && tCour == ENT) || (tabSymb[tmp].type == BOOL && tCour == BOOL))) {
				po.produire(AFFECTERG);
				po.produire(tabSymb[tmp].info);
			} else {
				UtilLex.messErr("Mauvais type ou var inexistante ptgen50");
			}
			break;
		case 51:
			affect = presentIdent(1);
			break;
			
		/**** Conditions ****/
			/**** si ****/
		case 100: // Verification expression bool + production MAPILE "si" + preparation pile reprise
			verifBool();
			po.produire(BSIFAUX);
			po.produire(-1);
			pileRep.empiler(po.getIpo());
			break;
		case 101: // production code MAPILE "sinon"
			po.produire(BINCOND);
			po.produire(-1);
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			pileRep.empiler(po.getIpo());
			break;
		case 102: // depilement pile reprise + mise a jour code MAPILE
			po.modifier(pileRep.depiler(), po.getIpo()+1);
			break;
			
			/**** ttq ****/
			
		case 110: // preparation pile reprise
			pileRep.empiler(po.getIpo()+1);
			break;
		case 111: // Verification expression bool + production MAPILE "ttq" + actualisation pile reprise
			verifBool();
			po.produire(BSIFAUX);
			po.produire(-1);
			pileRep.empiler(po.getIpo());
			break;
		case 112: // depilement pile reprise + mise a jour code MAPILE
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(pileRep.depiler());
			break;
		
			/**** cond ****/
		
		case 120: // preparation pile reprise avec 0
			pileRep.empiler(0);
			break;
		case 121: // verif bool + mise a jour pile reprise + production MAPILE "cond"
			verifBool();
			po.produire(BSIFAUX);
			po.produire(-1);
			pileRep.empiler(po.getIpo());
			break;
		case 122: // actualisation pile reprise + mise a jour code MAPILE
			po.modifier(pileRep.depiler(), po.getIpo()+3);
			po.produire(BINCOND);
			po.produire(pileRep.depiler());
			pileRep.empiler(po.getIpo());
			break;
		case 123: // mise a jour pile reprise
			int bincond = po.getIpo()+1;
			int elt = pileRep.depiler();
			while(po.getElt(elt) != 0) {
				
			}
			break;

		/**** Arret ****/
			
		case 999:
			po.produire(ARRET);
			break;
		
		
		// A COMPLETER

		default:
			System.out.println("Point de generation non prevu dans votre liste");
			break;

		}
		afftabSymb();
		po.constObj();
		po.constGen();
	}
}
