package com.santosediego.donorregistry.repositories;

/**
 * Pieces of the donor search query, defined once. They are compile-time
 * constants, so they can be concatenated inside @Query annotations.
 *
 * Accent-insensitive search: TRANSLATE runs first (accented letters, lower and
 * upper case, to plain letters) and LOWER runs after, so the result does not
 * depend on the database locale. Both the column and the search term go through
 * the same expression.
 */
public final class DonorSearchQueries {

	public static final String ACCENTED = "áàâãäéèêëíìîïóòôõöúùûüçñÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇÑ";
	public static final String PLAIN = "aaaaaeeeeiiiiooooouuuucn" + "AAAAAEEEEIIIIOOOOOUUUUCN";

	public static final String NORMALIZED_NAME = "LOWER(TRANSLATE(d.name, '" + ACCENTED + "', '" + PLAIN + "'))";
	public static final String NORMALIZED_TERM = "LOWER(TRANSLATE(:name, '" + ACCENTED + "', '" + PLAIN + "'))";

	// Escape character for LIKE, so a typed % or _ is a literal, not a wildcard
	public static final char LIKE_ESCAPE = '!';
	public static final String ESCAPE_CLAUSE = " ESCAPE '" + LIKE_ESCAPE + "'";

	/*
	 * Filter of the donor list. No parameter is ever null: PostgreSQL cannot infer
	 * the type of a null parameter. The service sends "filtered = false" (and empty
	 * patterns) when there is no search term. The patterns come from likePattern.
	 */
	public static final String WHERE = " WHERE (:filtered = false"
			+ " OR " + NORMALIZED_NAME + " LIKE " + NORMALIZED_TERM + ESCAPE_CLAUSE
			+ " OR (:cpfSearch = true AND d.cpf LIKE :cpf" + ESCAPE_CLAUSE + ")"
			+ " OR LOWER(d.rg) LIKE :rg" + ESCAPE_CLAUSE + ")";

	private DonorSearchQueries() {
	}

	/** Turns a typed term into a LIKE pattern "%term%", with %, _ and the escape character taken literally. */
	public static String likePattern(String term) {
		String escape = String.valueOf(LIKE_ESCAPE);
		String escaped = term.replace(escape, escape + escape).replace("%", escape + "%").replace("_", escape + "_");
		return "%" + escaped + "%";
	}
}
