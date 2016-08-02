package rega.genotype.ui.viruses.hpv;

import java.awt.Color;

import rega.genotype.ui.data.OrganismDefinition;
import rega.genotype.ui.util.DefaultGenomeAttributes;

/**
 * HIV genome map drawing implementation.
 * 
 * @author plibin0
 */
public class HpvGenome extends DefaultGenomeAttributes {
	
	public HpvGenome(OrganismDefinition od) {
		super(od);
		getColors().put("alpha",new Color(0xff,0x00,0x00));
		getColors().put("alpha1",new Color(0xef,0x00,0x00));
		getColors().put("alpha2",new Color(0xdf,0x00,0x00));
		getColors().put("alpha3",new Color(0xcf,0x00,0x00));
		getColors().put("alpha4",new Color(0xbf,0x00,0x00));
		getColors().put("alpha5",new Color(0xaf,0x00,0x00));
		getColors().put("alpha6",new Color(0x9f,0x00,0x00));
		getColors().put("alpha7",new Color(0x8f,0x00,0x00));
		getColors().put("alpha8",new Color(0x7f,0x00,0x00));
		getColors().put("alpha9",new Color(0x6f,0x00,0x00));
		getColors().put("alpha10",new Color(0x5f,0x00,0x00));
		getColors().put("alpha11",new Color(0x4f,0x00,0x00));
		getColors().put("alpha12",new Color(0x3f,0x00,0x00));
		getColors().put("alpha13",new Color(0x2f,0x00,0x00));
		getColors().put("alpha14",new Color(0x1f,0x00,0x00));
		getColors().put("alpha15",new Color(0x0f,0x00,0x00));
		getColors().put("beta",new Color(0x00,0xff,0x00));
		getColors().put("beta1",new Color(0x00,0x80,0x00));
		getColors().put("beta2",new Color(0x00,0xc0,0x00));
		getColors().put("beta3",new Color(0x00,0x40,0x00));
		getColors().put("beta4",new Color(0x00,0x20,0x00));
		getColors().put("beta5",new Color(0x00,0x60,0x00));
		getColors().put("gamma",new Color(0x00,0xa0,0x00));
		getColors().put("gamma1",new Color(0x00,0xe0,0x00));
		getColors().put("gamma2",new Color(0x00,0x00,0xff));
		getColors().put("gamma3",new Color(0x00,0x00,0x80));
		getColors().put("gamma4",new Color(0x00,0x00,0xc0));
		getColors().put("delta",new Color(0x00,0x00,0x40));
		getColors().put("delta1",new Color(0x00,0x00,0x20));
		getColors().put("delta2",new Color(0x00,0x00,0x60));
		getColors().put("delta3",new Color(0x00,0x00,0xa0));
		getColors().put("delta4",new Color(0x00,0x00,0xe0));
		getColors().put("epsilon",new Color(0xff,0xff,0x00));
		getColors().put("epsilon1",new Color(0x80,0x80,0x00));
		getColors().put("zeta",new Color(0xc0,0xc0,0x00));
		getColors().put("zeta1",new Color(0x40,0x40,0x00));
		getColors().put("eta",new Color(0x20,0x20,0x00));
		getColors().put("eta1",new Color(0x60,0x60,0x00));
		getColors().put("theta",new Color(0xa0,0xa0,0x00));
		getColors().put("theta1",new Color(0xe0,0xe0,0x00));
		getColors().put("iota",new Color(0xff,0x00,0xff));
		getColors().put("iota1",new Color(0x80,0x00,0x80));
		getColors().put("kappa",new Color(0xc0,0x00,0xc0));
		getColors().put("kappa1",new Color(0x40,0x00,0x40));
		getColors().put("kappa2",new Color(0x20,0x00,0x20));
		getColors().put("lambda",new Color(0x60,0x00,0x60));
		getColors().put("lambda1",new Color(0xa0,0x00,0xa0));
		getColors().put("lambda2",new Color(0xe0,0x00,0xe0));
		getColors().put("lambda3",new Color(0x00,0xff,0xff));
		getColors().put("mu",new Color(0x00,0x80,0x80));
		getColors().put("mu1",new Color(0x00,0xc0,0xc0));
		getColors().put("mu2",new Color(0x00,0x40,0x40));
		getColors().put("nu",new Color(0x00,0x20,0x20));
		getColors().put("nu1",new Color(0x00,0x60,0x60));
		getColors().put("xi",new Color(0x00,0xa0,0xa0));
		getColors().put("xi1",new Color(0x00,0xe0,0xe0));
		getColors().put("omikron",new Color(0x00,0x00,0x00));
		getColors().put("omikron1",new Color(0x80,0x80,0x80));
		getColors().put("pi",new Color(0xc0,0xc0,0xc0));
		getColors().put("pi1",new Color(0x40,0x40,0x40));
		getColors().put("rho",new Color(0x20,0x20,0x20));
		getColors().put("rho1",new Color(0x60,0x60,0x60));
		getColors().put("sigma",new Color(0xa0,0xa0,0xa0));
		getColors().put("sigma1",new Color(0xe0,0xe0,0xe0));
		getColors().put("unclassified1",new Color(0x00,0x80,0xff));
		getColors().put("unclassified2",new Color(0x00,0x40,0xff));


		getColors().put("-", new Color(0, 0xaa, 0xff));
	}
	
	public int getGenomeEnd() {
		return 7800;
	}

	public int getGenomeStart() {
		return 1;
	}

	public int getGenomeImageEndX() {
		return 579;
	}

	public int getGenomeImageStartX() {
		return -4;
	}

	public int getGenomeImageEndY() {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getGenomeImageStartY() {
		// TODO Auto-generated method stub
		return 0;
	}
}
