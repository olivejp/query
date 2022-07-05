package nc.deveo.query.service;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class for filtering attributes with {@link String} type.
 * It can be added to a criteria class as a member, to support the following query parameters:
 * <code>
 * fieldName.equals='something'
 * fieldName.notEquals='something'
 * fieldName.specified=true
 * fieldName.specified=false
 * fieldName.in='something','other'
 * fieldName.notIn='something','other'
 * fieldName.contains='thing'
 * fieldName.startsWith='thing'
 * fieldName.endsWith='thing'
 * fieldName.containsIn='something','other'
 * fieldName.notContains='thing'
 * fieldName.equals=42
 * fieldName.notEquals=42
 * fieldName.specified=true
 * fieldName.specified=false
 * fieldName.in=43,42
 * fieldName.notIn=43,42
 * fieldName.greaterThan=41
 * fieldName.lessThan=44
 * fieldName.greaterThanOrEqual=42
 * fieldName.lessThanOrEqual=44
 * </code>
 */
public class GenericFilter<FIELD_TYPE extends Comparable<? super FIELD_TYPE>> {

    private FIELD_TYPE equals;
    private FIELD_TYPE notEquals;
    private Boolean specified;
    private List<FIELD_TYPE> in;
    private List<FIELD_TYPE> notIn;
    private FIELD_TYPE greaterThan;
    private FIELD_TYPE lessThan;
    private FIELD_TYPE greaterThanOrEqual;
    private FIELD_TYPE lessThanOrEqual;
    private String contains;
    private String notContains;
    private String startsWith;
    private String endsWith;
    private List<String> containsIn;
    private List<Pair<List<String>, List<String>>> propertiesToSearch;

    /**
     * <p>Constructor for GenericFilter.</p>
     */
    public GenericFilter() {
    }

    /**
     * <p>Constructor for GenericFilter.</p>
     *
     * @param filter a {@link GenericFilter} object.
     */
    public GenericFilter(final GenericFilter<FIELD_TYPE> filter) {
        this.equals = filter.equals;
        this.notEquals = filter.notEquals;
        this.specified = filter.specified;
        this.in = filter.in == null ? null : new ArrayList<>(filter.in);
        this.notIn = filter.notIn == null ? null : new ArrayList<>(filter.notIn);
        this.contains = filter.contains;
        this.notContains = filter.notContains;
        this.containsIn = filter.containsIn;
        this.startsWith = filter.startsWith;
        this.endsWith = filter.endsWith;
        this.greaterThan = filter.greaterThan;
        this.lessThan = filter.lessThan;
        this.greaterThanOrEqual = filter.greaterThanOrEqual;
        this.lessThanOrEqual = filter.lessThanOrEqual;
        this.propertiesToSearch = filter.propertiesToSearch;
    }

    /**
     * <p>copy.</p>
     *
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> copy() {
        return new GenericFilter<>(this);
    }

    /**
     * <p>Getter for the field <code>equals</code>.</p>
     *
     * @return a FIELD_TYPE object.
     */
    public FIELD_TYPE getEquals() {
        return equals;
    }

    /**
     * <p>Setter for the field <code>equals</code>.</p>
     *
     * @param equals a FIELD_TYPE object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setEquals(FIELD_TYPE equals) {
        this.equals = equals;
        return this;
    }

    /**
     * <p>Getter for the field <code>notEquals</code>.</p>
     *
     * @return a FIELD_TYPE object.
     */
    public FIELD_TYPE getNotEquals() {
        return notEquals;
    }

    /**
     * <p>Setter for the field <code>notEquals</code>.</p>
     *
     * @param notEquals a FIELD_TYPE object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setNotEquals(FIELD_TYPE notEquals) {
        this.notEquals = notEquals;
        return this;
    }

    /**
     * <p>Getter for the field <code>specified</code>.</p>
     *
     * @return a {@link Boolean} object.
     */
    public Boolean getSpecified() {
        return specified;
    }

    /**
     * <p>Setter for the field <code>specified</code>.</p>
     *
     * @param specified a {@link Boolean} object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setSpecified(Boolean specified) {
        this.specified = specified;
        return this;
    }

    /**
     * <p>Getter for the field <code>in</code>.</p>
     *
     * @return a {@link List} object.
     */
    public List<FIELD_TYPE> getIn() {
        return in;
    }

    /**
     * <p>Setter for the field <code>in</code>.</p>
     *
     * @param in a {@link List} object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setIn(List<FIELD_TYPE> in) {
        this.in = in;
        return this;
    }

    /**
     * <p>Getter for the field <code>notIn</code>.</p>
     *
     * @return a {@link List} object.
     */
    public List<FIELD_TYPE> getNotIn() {
        return notIn;
    }

    /**
     * <p>Setter for the field <code>notIn</code>.</p>
     *
     * @param notIn a {@link List} object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setNotIn(List<FIELD_TYPE> notIn) {
        this.notIn = notIn;
        return this;
    }

    /**
     * <p>Getter for the field <code>contains</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getContains() {
        return contains;
    }

    /**
     * <p>Setter for the field <code>contains</code>.</p>
     *
     * @param contains a {@link String} object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setContains(String contains) {
        this.contains = contains;
        return this;
    }

    /**
     * <p>Getter for the field <code>containsIn</code>.</p>
     *
     * @return a {@link List <String>} object.
     */
    public List<String> getContainsIn() {
        return containsIn;
    }

    /**
     * <p>Setter for the field <code>containsIn</code>.</p>
     *
     * @param containsIn a {@link List <String>} object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setContainsIn(List<String> containsIn) {
        this.containsIn = containsIn;
        return this;
    }

    /**
     * <p>Getter for the field <code>notContains</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getDoesNotContain() {
        return notContains;
    }

    /**
     * <p>Setter for the field <code>notContains</code>.</p>
     *
     * @param notContains a {@link String} object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setDoesNotContain(String notContains) {
        this.notContains = notContains;
        return this;
    }

    public String getStartsWith() {
        return startsWith;
    }

    public GenericFilter<FIELD_TYPE> setStartsWith(String startsWith) {
        this.startsWith = startsWith;
        return this;
    }

    public String getEndsWith() {
        return endsWith;
    }

    public GenericFilter<FIELD_TYPE> setEndsWith(String endsWith) {
        this.endsWith = endsWith;
        return this;
    }

    /**
     * <p>Getter for the field <code>greaterThan</code>.</p>
     *
     * @return a FIELD_TYPE object.
     */
    public FIELD_TYPE getGreaterThan() {
        return greaterThan;
    }

    /**
     * <p>Setter for the field <code>greaterThan</code>.</p>
     *
     * @param greaterThan a FIELD_TYPE object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setGreaterThan(FIELD_TYPE greaterThan) {
        this.greaterThan = greaterThan;
        return this;
    }

    /**
     * <p>Getter for the field <code>lessThan</code>.</p>
     *
     * @return a FIELD_TYPE object.
     */
    public FIELD_TYPE getLessThan() {
        return lessThan;
    }

    /**
     * <p>Setter for the field <code>lessThan</code>.</p>
     *
     * @param lessThan a FIELD_TYPE object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setLessThan(FIELD_TYPE lessThan) {
        this.lessThan = lessThan;
        return this;
    }

    /**
     * <p>Getter for the field <code>greaterThanOrEqual</code>.</p>
     *
     * @return a FIELD_TYPE object.
     */
    public FIELD_TYPE getGreaterThanOrEqual() {
        return greaterThanOrEqual;
    }

    /**
     * <p>Setter for the field <code>greaterThanOrEqual</code>.</p>
     *
     * @param greaterThanOrEqual a FIELD_TYPE object.
     */
    public void setGreaterThanOrEqual(FIELD_TYPE greaterThanOrEqual) {
        this.greaterThanOrEqual = greaterThanOrEqual;
    }

    /**
     * <p>Setter for the field <code>greaterThanOrEqual</code>.</p>
     *
     * @param greaterThanOrEqual a FIELD_TYPE object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setGreaterOrEqualThan(FIELD_TYPE greaterThanOrEqual) {
        this.setGreaterThanOrEqual(greaterThanOrEqual);
        return this;
    }

    /**
     * <p>Getter for the field <code>lessThanOrEqual</code>.</p>
     *
     * @return a FIELD_TYPE object.
     */
    public FIELD_TYPE getLessThanOrEqual() {
        return lessThanOrEqual;
    }

    /**
     * <p>Setter for the field <code>lessThanOrEqual</code>.</p>
     *
     * @param lessThanOrEqual a FIELD_TYPE object.
     */
    public void setLessThanOrEqual(FIELD_TYPE lessThanOrEqual) {
        this.lessThanOrEqual = lessThanOrEqual;
    }

    /**
     * <p>Setter for the field <code>lessThanOrEqual</code>.</p>
     *
     * @param lessThanOrEqual a FIELD_TYPE object.
     * @return a {@link GenericFilter} object.
     */
    public GenericFilter<FIELD_TYPE> setLessOrEqualThan(FIELD_TYPE lessThanOrEqual) {
        this.setLessThanOrEqual(lessThanOrEqual);
        return this;
    }

    public List<Pair<List<String>, List<String>>> getPropertiesToSearch() {
        return propertiesToSearch;
    }

    public void setPropertiesToSearch(List<Pair<List<String>, List<String>>> propertiesToSearch) {
        this.propertiesToSearch = propertiesToSearch;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GenericFilter<?> that = (GenericFilter<?>) o;
        return Objects.equals(equals, that.equals) &&
                Objects.equals(notEquals, that.notEquals) &&
                Objects.equals(specified, that.specified) &&
                Objects.equals(in, that.in) &&
                Objects.equals(notIn, that.notIn) &&
                Objects.equals(contains, that.contains) &&
                Objects.equals(containsIn, that.containsIn) &&
                Objects.equals(startsWith, that.startsWith) &&
                Objects.equals(endsWith, that.endsWith) &&
                Objects.equals(notContains, that.notContains) &&
                Objects.equals(greaterThan, that.greaterThan) &&
                Objects.equals(lessThan, that.lessThan) &&
                Objects.equals(greaterThanOrEqual, that.greaterThanOrEqual) &&
                Objects.equals(lessThanOrEqual, that.lessThanOrEqual);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(equals, notEquals, specified, in, notIn, contains, containsIn, notContains, startsWith,
                endsWith, greaterThan, lessThan, greaterThanOrEqual, lessThanOrEqual);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getFilterName() + " ["
                + (getEquals() != null ? "equals=" + getEquals() + ", " : "")
                + (getNotEquals() != null ? "notEquals=" + getNotEquals() + ", " : "")
                + (getSpecified() != null ? "specified=" + getSpecified() + ", " : "")
                + (getIn() != null ? "in=" + getIn() + ", " : "")
                + (getNotIn() != null ? "notIn=" + getNotIn() + ", " : "")
                + (getContains() != null ? "contains=" + getContains() + ", " : "")
                + (getStartsWith() != null ? "startsWith=" + getStartsWith() + ", " : "")
                + (getEndsWith() != null ? "endsWith=" + getEndsWith() + ", " : "")
                + (getContainsIn() != null ? "containsIn=" + getContainsIn() + ", " : "")
                + (getDoesNotContain() != null ? "notContains=" + getDoesNotContain() : "")
                + (getGreaterThan() != null ? "greaterThan=" + getGreaterThan() + ", " : "")
                + (getLessThan() != null ? "lessThan=" + getLessThan() + ", " : "")
                + (getGreaterThanOrEqual() != null ? "greaterThanOrEqual=" + getGreaterThanOrEqual() + ", " : "")
                + (getLessThanOrEqual() != null ? "lessThanOrEqual=" + getLessThanOrEqual() : "")
                + "]";
    }

    /**
     * <p>getFilterName.</p>
     *
     * @return a {@link String} object.
     */
    protected String getFilterName() {
        return this.getClass().getSimpleName();
    }

}
