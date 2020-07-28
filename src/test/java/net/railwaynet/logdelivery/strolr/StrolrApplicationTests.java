package net.railwaynet.logdelivery.strolr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class StrolrApplicationTests {

	private RailroadsService rs;

	@BeforeEach
	void init() {
		rs = new RailroadsService();
	}

	@Test
	void getRailroads() {
		Map<?, ?> result = rs.getRailroadsBySCAC("AMTK");
		assert(result.toString().equals("{AccessSCAC=AMTK, SCAC=[{label=AMTK, options=[AMTK, CDTX, WDTX, IDTX, RNCX], federations=[amtk]}], Plugins=[STROS, STROB]}"));
	}

	@Test
	void getFederations() {
		List<String> result = rs.getFederationsBySCAC("AMTK");
		assert(result.toString().equals("[amtk]"));
	}

	@Test
	void getSCAC() {
		String result = rs.getSCACbyMARK("AMTK", "IDTX");
		assert(result.equals("AMTK"));
		result = rs.getSCACbyMARK("RCAX", "IDTX");
		assert(result.equals("AMTK"));
	}

}
