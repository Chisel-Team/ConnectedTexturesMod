package team.chisel.ctm.client.util;

import java.util.Objects;

import it.unimi.dsi.fastutil.Hash.Strategy;

public class IdentityStrategy<K> implements Strategy<K> {

	@Override
	public int hashCode(K o) {
		return Objects.hashCode(o);
	}

	@Override
	public boolean equals(K a, K b) {
		return Objects.equals(a, b);
	}

}
