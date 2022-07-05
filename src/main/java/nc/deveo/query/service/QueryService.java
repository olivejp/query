package nc.deveo.query.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.*;
import javax.persistence.criteria.CriteriaBuilder.In;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Base service for constructing and executing complex queries.
 *
 * @param <E> the type of the E which is queried.
 * @param <R> the type of bean extending JpaSpecificationExecutor.
 *            Most of the time, should be a repository.
 */
@Transactional(readOnly = true)
@Log4j2
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public abstract class QueryService<E, R extends JpaSpecificationExecutor<E>> {

    public static final List<String> LIST_ARRAY_OPERATOR = List.of("in", "notIn", "containsIn");
    public static final String SEARCH_PARAMS_REGEX = "^([\\w.,/]+?)(?>\\|(\\w+))??$";

    private final R repository;

    /**
     * Method which should return the Class<E>.
     *
     * @return Class
     */
    @NonNull
    protected abstract Class<E> getType();

    /**
     * Method to implement if you need to initialize hibernate lazy dependencies.
     * eg:
     * protected Consumer<Contrat> initializeLazyDependencies() {
     * return contrat -> {
     * Hibernate.initialize(contrat.getListFacture());
     * Hibernate.initialize(contrat.getSetObservation());
     * };
     * }
     *
     * @return Consumer
     */
    @Nullable
    protected Consumer<E> initializeLazyDependencies() {
        return null;
    }

    public Page<E> findByCriteria(Map<String, String> allParams, Pageable page) {
        Specification<E> specification = buildAllGenericSpecification(getType(), allParams, page);
        Pageable customPage = PageRequest.of(page.getPageNumber(), page.getPageSize());
        Page<E> all = repository.findAll(specification, customPage);
        Consumer<E> consumer = this.initializeLazyDependencies();
        if (consumer != null) {
            all.getContent().forEach(consumer);
        }

        return all;
    }

    protected <F extends Comparable<? super F>> Specification<E> buildAllGenericSpecification(Class<E> searchedEntityClass,
                                                                                              Map<String, String> allFieldsToSearch,
                                                                                              Pageable page) {
        Specification<E> specification = ((root, query, criteriaBuilder) -> {
            List<Expression<?>> expressionList = new ArrayList<>();
            expressionList.add(root.get("id"));

            List<Order> sortList = page.getSort().stream().map(sort -> {
                List<String> sortPropertyPath = List.of(sort.getProperty().split("\\."));
                Expression<?> sortExpression = getExpression(sortPropertyPath, root);
                expressionList.add(sortExpression);
                return sort.getDirection() == Sort.Direction.ASC ? criteriaBuilder.asc(sortExpression) : criteriaBuilder.desc(sortExpression);
            }).collect(Collectors.toList());
            // avoir sql error
            query.orderBy(sortList);

            // avoid duplicate
            query.groupBy(expressionList);
            return null;
        });

        if (allFieldsToSearch == null) {
            return specification;
        }

        List<GenericFilter<F>> filters = new ArrayList<>();
        buildEntityFilters(searchedEntityClass, filters, allFieldsToSearch);
        try {
            for (GenericFilter<F> filter : filters) {
                specification = specification.and(buildGenericEntitySpecification(filter, searchedEntityClass));
            }
        } catch (NoSuchFieldException ex) {
            log.error(ex);
            throw new RuntimeException(ex.getLocalizedMessage());
        }

        return specification;
    }

    public <F extends Comparable<? super F>> void buildEntityFilters(Class<E> searchedEntityClass,
                                                                     List<GenericFilter<F>> filters,
                                                                     Map<String, String> allFieldsToSearch) {
        Field[] allSearchedEntityFields = FieldUtils.getAllFields(searchedEntityClass);
        Pattern pattern = Pattern.compile(SEARCH_PARAMS_REGEX);
        for (Map.Entry<String, String> fieldToSearchMap : allFieldsToSearch.entrySet()) {
            Matcher matcher = pattern.matcher(fieldToSearchMap.getKey());
            if (matcher.find()) {
                List<Field> fields = new ArrayList<>();
                List<List<String>> properties = new ArrayList<>();
                List<List<String>> subProperties = new ArrayList<>();
                List<String> searchOperators = new ArrayList<>();

                // Liste des champs à rechercher avec l'opérateur OU
                List<String> termsToApplyOrOperator = new ArrayList<>(List.of(matcher.group(1).split("/")));

                for (String terms : termsToApplyOrOperator) {
                    List<String> searchedPropertyPath = new ArrayList<>(List.of(terms.split("\\.")));

                    String rootPropertyName = searchedPropertyPath.get(0).split(",")[0];

                    Optional<Field> optionalFieldCorrespondingToRootPropertyName = Arrays.stream(allSearchedEntityFields).filter(field -> field.getName().equals(rootPropertyName)).findFirst();

                    // Si la propriété recherchée n'existe pas dans la liste des propriétés de la classe mère,
                    // on passe à l'itération suivante sans rien faire
                    if (optionalFieldCorrespondingToRootPropertyName.isEmpty()) {
                        continue;
                    }
                    fields.add(optionalFieldCorrespondingToRootPropertyName.get());

                    List<String> additionnalPropertiesToSearchWithOrOperator = List.of(searchedPropertyPath.get(searchedPropertyPath.size() - 1).split(","));
                    if (additionnalPropertiesToSearchWithOrOperator.size() > 1) {
                        searchedPropertyPath.set(searchedPropertyPath.size() - 1, searchedPropertyPath.get(searchedPropertyPath.size() - 1).split(",")[0]);
                    }
                    properties.add(searchedPropertyPath);
                    subProperties.add(additionnalPropertiesToSearchWithOrOperator);
                    searchOperators.add(matcher.group(2) != null ? matcher.group(2) : (additionnalPropertiesToSearchWithOrOperator.size() > 1 ? "containsIn" : "equals"));
                }
                if (properties.size() > 0) {
                    buildFilterList(filters, fieldToSearchMap, fields, properties, subProperties, searchOperators);
                }
            }
        }
    }

    private Field getLastDeclaredField(Field field, List<String> propertyList) throws NoSuchFieldException {
        if (propertyList.size() == 1) {
            return field;
        }
        Field declaredField = field;
        for (int i = 1; i < propertyList.size(); i++) {
            if (declaredField.getType().getSimpleName().equals("List")) {
                declaredField = ((Class<?>) ((ParameterizedType) declaredField.getGenericType()).getActualTypeArguments()[0]).getDeclaredField(propertyList.get(i));
            } else {
                declaredField = declaredField.getType().getDeclaredField(propertyList.get(i));
            }
        }
        return declaredField;
    }

    protected <F extends Comparable<? super F>> void buildFilterList(List<GenericFilter<F>> filters,
                                                                     Map.Entry<String, String> fieldToSearchMap,
                                                                     List<Field> fields,
                                                                     List<List<String>> properties,
                                                                     List<List<String>> subProperties,
                                                                     List<String> searchOperators) {
        try {
            GenericFilter<F> genericFilter = new GenericFilter<>();
            PropertyAccessor genericFilterPropertyAccessor = PropertyAccessorFactory.forDirectFieldAccess(genericFilter);

            for (int i = 0; i < properties.size(); i++) {
                //Recuperation de la reference de l'attribut le plus bas exemple
                // exemple => produit.agence.code, on récupére le code.
                Field declaredField = getLastDeclaredField(fields.get(i), properties.get(i));
                Class<?> entityPropertyType = declaredField.getType();

                final boolean isOperatorExist = genericFilterPropertyAccessor.getPropertyType(searchOperators.get(i)) != null;

                if (!isOperatorExist) {
                    throw new RuntimeException("Operator de recherche n'est pas valide :" + searchOperators.get(i));
                }
                if (LIST_ARRAY_OPERATOR.contains(searchOperators.get(i))) {
                    writeInFilterForArrayValue(genericFilterPropertyAccessor, entityPropertyType, fieldToSearchMap, searchOperators.get(i));
                } else {
                    writeInFilterForSimpleValue(genericFilterPropertyAccessor, entityPropertyType, fieldToSearchMap, searchOperators.get(i));
                }
                if (genericFilter.getPropertiesToSearch() == null) {
                    genericFilter.setPropertiesToSearch(new ArrayList<>());
                }
                Pair<List<String>, List<String>> pair = new MutablePair<>(properties.get(i), subProperties.get(i).size() > 0 ? subProperties.get(i) : null);
                genericFilter.getPropertiesToSearch().add(pair);
            }
            filters.add(genericFilter);
        } catch (ParseException | NoSuchFieldException | IllegalArgumentException ex) {
            throw new RuntimeException(ex.getLocalizedMessage());
        }
    }

    protected void writeInFilterForArrayValue(PropertyAccessor criteriaFilterPropertyAccessor,
                                              Class<?> entityPropertyType,
                                              Map.Entry<String, String> param,
                                              String searchOperator) throws ParseException {
        if (entityPropertyType.isEnum()) {
            List<Enum> listEnumValue = Arrays.stream(param.getValue().split(","))
                    .map(enumValue -> Enum.valueOf((Class<? extends Enum>) entityPropertyType, enumValue))
                    .collect(Collectors.toList());
            criteriaFilterPropertyAccessor.setPropertyValue(searchOperator, listEnumValue);
        } else {
            List<Object> valueList = parseAndCastList(param.getValue().split(","), entityPropertyType);
            criteriaFilterPropertyAccessor.setPropertyValue(searchOperator, valueList);
        }
    }

    protected void writeInFilterForSimpleValue(PropertyAccessor genericFilterPropertyAccessor,
                                               Class<?> entityPropertyType,
                                               Map.Entry<String, String> param,
                                               String searchOperator) throws ParseException {
        if (entityPropertyType.isEnum()) {

            Object enumValue = searchOperator.equals("specified") ? param.getValue() : Enum.valueOf((Class<? extends Enum>) entityPropertyType, param.getValue());
            genericFilterPropertyAccessor.setPropertyValue(searchOperator, enumValue);

        } else {
            Object value = searchOperator.equals("specified") ? param.getValue() : parseAndCastValue(param.getValue(), entityPropertyType);
            genericFilterPropertyAccessor.setPropertyValue(searchOperator, value);
        }
    }

    protected List<Object> parseAndCastList(String[] termList, Class<?> entityPropertyType) throws ParseException {
        List<Object> valueList = new ArrayList<>();
        for (String term : termList) {
            valueList.add(parseAndCastValue(term, entityPropertyType));
        }
        return valueList;
    }

    protected Object parseAndCastValue(String term, Class<?> entityPropertyType) throws ParseException {
        return switch (entityPropertyType.getSimpleName()) {
            case "UUID" -> UUID.fromString(term);
            case "Integer" -> Integer.parseInt(term);
            case "Short" -> Short.parseShort(term);
            case "Long" -> Long.parseLong(term);
            case "Boolean" -> Boolean.parseBoolean(term);
            case "Duration" -> Duration.parse(term);
            case "Float" -> Float.parseFloat(term);
            case "Double" -> Double.parseDouble(term);
            case "Instant" -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(term).toInstant();
            case "LocalDate" ->
                    LocalDate.ofInstant(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(term).toInstant(), ZoneId.systemDefault());
            case "ZonedDateTime" ->
                    ZonedDateTime.ofInstant(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(term).toInstant(), ZoneId.systemDefault());
            default -> term;
        };
    }

    private <F extends Comparable<? super F>> boolean parseBoolean(F value) {
        if ("0".equals(value) || "no".equalsIgnoreCase((String) value) || "non".equalsIgnoreCase((String) value) || "faux".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)) {
            return false;
        } else if ("1".equals(value) || "yes".equalsIgnoreCase((String) value) || "oui".equalsIgnoreCase((String) value) || "vrai".equalsIgnoreCase((String) value) || "true".equalsIgnoreCase((String) value)) {
            return true;
        } else {
            throw new java.lang.IllegalArgumentException("Invalid string: " + value + " can't be converted into true or false!");
        }
    }

    /**
     * Helper function to return a specification for filtering on a single {@link java.lang.Comparable}, where equality, less
     * than, greater than and less-than-or-equal-to and greater-than-or-equal-to and null/non-null conditions are
     * supported.
     *
     * @param <F>               The type of the attribute which is filtered.
     * @param filter            the individual attribute filter coming from the frontend.
     * @param metaclassFunction lambda, which based on a Root&lt;E&gt; returns Expression - basicaly picks a column
     * @return a Specification
     */
    protected <F extends Comparable<? super F>> Specification<E> buildSpecification(GenericFilter<F> filter, Function<Root<E>, Expression<F>> metaclassFunction, Field field) {
        if (filter.getEquals() != null) {
            if (filter.getEquals() instanceof Instant || filter.getEquals() instanceof ZonedDateTime || filter.getEquals() instanceof LocalDate || filter.getEquals() instanceof LocalDateTime) {
                return equalsDateSpecification(metaclassFunction, filter.getEquals());
            } else if (field.getType().isInstance("")) {
                return equalsStringSpecification(metaclassFunction, filter.getEquals().toString());
            } else if (field.getType().equals(boolean.class)) {
                return equalsBooleanSpecification(metaclassFunction, parseBoolean(filter.getEquals()));
            } else {
                return equalsSpecification(metaclassFunction, filter.getEquals());
            }
        } else if (filter.getIn() != null) {
            return valueIn(metaclassFunction, filter.getIn());
        }

        Specification<E> result = Specification.where(null);
        if (filter.getNotEquals() != null) {
            if (filter.getEquals() instanceof Instant || filter.getEquals() instanceof ZonedDateTime || filter.getEquals() instanceof LocalDate || filter.getEquals() instanceof LocalDateTime) {
                result = result.and(notEqualsDateSpecification(metaclassFunction, filter.getNotEquals()));
            }
            boolean isString = field.getType().isInstance("");
            if (isString) {
                result = result.and(notEqualsStringSpecification(metaclassFunction, filter.getNotEquals().toString()));
            } else {
                result = result.and(notEqualsSpecification(metaclassFunction, filter.getNotEquals()));
            }
        }
        if (filter.getSpecified() != null) {
            result = result.and(byFieldSpecified(metaclassFunction, filter.getSpecified()));
        }
        if (filter.getNotIn() != null) {
            result = result.and(valueNotIn(metaclassFunction, filter.getNotIn()));
        }
        if (filter.getGreaterThan() != null) {
            result = result.and(greaterThan(metaclassFunction, filter.getGreaterThan()));
        }
        if (filter.getGreaterThanOrEqual() != null) {
            result = result.and(greaterThanOrEqualTo(metaclassFunction, filter.getGreaterThanOrEqual()));
        }
        if (filter.getLessThan() != null) {
            result = result.and(lessThan(metaclassFunction, filter.getLessThan()));
        }
        if (filter.getLessThanOrEqual() != null) {
            result = result.and(lessThanOrEqualTo(metaclassFunction, filter.getLessThanOrEqual()));
        }
        if (filter.getContains() != null) {
            result = result.and(likeUpperSpecification(metaclassFunction, filter.getContains()));
        }
        if (filter.getStartsWith() != null) {
            result = result.and(startsWith(metaclassFunction, filter.getStartsWith()));
        }
        if (filter.getEndsWith() != null) {
            result = result.and(endsWith(metaclassFunction, filter.getEndsWith()));
        }
        if (filter.getContainsIn() != null) {
            result = result.and(likeUpperSpecificationIn(metaclassFunction, filter.getContainsIn()));
        }
        if (filter.getDoesNotContain() != null) {
            result = result.and(notContainsSpecification(metaclassFunction, filter.getDoesNotContain()));
        }
        return result;
    }

    protected <F extends Comparable<? super F>> Specification<E> buildGenericEntitySpecification(GenericFilter<F> filter, Class<E> searchedEntityClass) throws NoSuchFieldException {
        List<Pair<List<String>, List<String>>> propertiesToSearch = filter.getPropertiesToSearch();
        List<Specification<E>> specifications = new ArrayList<>();

        for (Pair<List<String>, List<String>> propertyToSearch : propertiesToSearch) {
            List<String> firstPropertyPath = propertyToSearch.getKey();
            List<String> subPropertiesNameList = propertyToSearch.getValue();

            // Ex contrat.titulaire
            Field parentFieldOfFirstPropertyFieldSearched = Arrays.stream(FieldUtils.getAllFields(searchedEntityClass)).filter(field -> field.getName().equals(firstPropertyPath.get(0))).findFirst().orElseThrow(() -> new NoSuchFieldException("Impossible de trouver le champ " + firstPropertyPath.get(0)));

            // Ex contrat.titulaire.prenom1
            Field firstPropertyFieldSearched = getLastDeclaredField(firstPropertyPath, searchedEntityClass);

            if (firstPropertyFieldSearched == null) {
                throw new NoSuchFieldException("Impossible de trouver le champ " + firstPropertyPath.get(firstPropertyPath.size() - 1));
            }

            // Si le champ recherché est un Set ou une Liste, alors la requete sera du type join
            boolean isJoin = parentFieldOfFirstPropertyFieldSearched.getType().getSimpleName().equals("List");

            if (subPropertiesNameList.size() == 1) {
                // Ex contrat.titulaire.prenom1
                specifications.add(buildSpecification(filter, root -> Objects.requireNonNull(isJoin ? getJoinExpression(firstPropertyPath, root) : getExpression(firstPropertyPath, root)), firstPropertyFieldSearched));
            } else {
                // Ex contrat.titulaire.prenom1,prenom2
                specifications.add((root, query, builder) -> {
                    List<Expression<F>> expressionList = new ArrayList<>();
                    if (isJoin) {
                        Join<E, F> propertyJoin = root.join(firstPropertyPath.get(0), JoinType.LEFT);
                        subPropertiesNameList.forEach(value -> expressionList.add(Objects.requireNonNull(getJoinExpression(firstPropertyPath, value, root, propertyJoin))));
                    } else {
                        subPropertiesNameList.forEach(value -> expressionList.add(Objects.requireNonNull(getExpression(firstPropertyPath, value, root))));
                    }
                    return buildStringPredicateMultiField(filter, expressionList, builder);
                });
            }
        }
        return specifications.stream().reduce(Specification::or).orElse(null);
    }

    private Field getLastDeclaredField(List<String> firstPropertyPath, Class<E> baseEntityClass) throws NoSuchFieldException {
        Field actualField = null;
        Class<?> searchedPropertyClass = baseEntityClass;
        for (String fieldName : firstPropertyPath) {
            boolean isList;
            if (actualField != null) {
                isList = actualField.getType().getSimpleName().equals("List");
                searchedPropertyClass = isList ? ((Class<?>) ((ParameterizedType) actualField.getGenericType()).getActualTypeArguments()[0]) : actualField.getType();
            }
            actualField = Arrays.stream(FieldUtils.getAllFields(searchedPropertyClass)).filter(field -> fieldName.equals(field.getName())).findFirst().orElseThrow(() -> new NoSuchFieldException("Impossible de trouver le champ: " + fieldName));
        }
        return actualField;
    }

    private <F extends Comparable<? super F>> Expression<F> getExpression(List<String> reference, Root<E> root) {
        Path<F> expression = null;
        for (String s : reference) {
            expression = (expression == null) ? root.get(s) : expression.get(s);
        }
        return expression;
    }

    private <F extends Comparable<? super F>> Expression<F> getJoinExpression(List<String> reference, Root<E> root) {
        Path<F> expression = null;
        for (String s : reference) {
            expression = (expression == null) ? root.join(s, JoinType.LEFT) : expression.get(s);
        }
        return expression;
    }

    private <F extends Comparable<? super F>> Expression<F> getExpression(List<String> reference, String value, Root<E> root) {
        Path<F> expression = null;
        if (reference.size() > 1) {
            for (int i = 0; i < reference.size() - 1; i++) {
                expression = (expression == null) ? root.get(reference.get(i)) : expression.get(reference.get(i));
            }
            return expression.get(value);
        } else {
            return root.get(value);
        }
    }

    private <F extends Comparable<? super F>> Expression<F> getJoinExpression(List<String> reference, String value, Root<E> root, Join<E, F> join) {
        Path<F> expression = join;
        if (reference.size() > 1) {
            for (int i = 1; i < reference.size() - 1; i++) {
                expression = expression.get(reference.get(i));
            }
            return expression.get(value);
        } else {
            return root.get(value);
        }
    }

    protected <F extends Comparable<? super F>> Predicate buildStringPredicateMultiField(GenericFilter filter, List<Expression<F>> fieldList, CriteriaBuilder cb) {
        if (filter.getContainsIn() != null) {
            return getSpecificationFromUpperConcatFields(filter.getContainsIn(), fieldList, cb);
        }
        return null;
    }

    protected <F extends Comparable<? super F>> Predicate getSpecificationFromUpperConcatFields(List<F> valueList, List<Expression<F>> listExpressionsToUpperConcat, CriteriaBuilder cb) {
        return valueList.stream().map(s -> likeUpperSpecificationUpperConcat(cb, listExpressionsToUpperConcat, s)).reduce(cb::and).orElse(null);
    }

    /**
     * Generic method, which based on a Root&lt;E&gt; returns an Expression which type is the same as the given 'value' type.
     *
     * @param metaclassFunction function which returns the column which is used for filtering.
     * @param value             the actual value to filter for.
     * @param <F>               The type of the attribute which is filtered.
     * @return a Specification.
     */
    protected <F extends Comparable<? super F>> Specification<E> equalsSpecification(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.equal(metaclassFunction.apply(root), value);
    }

    protected <F extends Comparable<? super F>> Specification<E> equalsBooleanSpecification(Function<Root<E>, Expression<F>> metaclassFunction, Boolean value) {
        return (root, query, builder) -> builder.equal(metaclassFunction.apply(root).as(Boolean.class), value);
    }

    protected <F extends Comparable<? super F>> Specification<E> equalsStringSpecification(Function<Root<E>, Expression<F>> metaclassFunction, String value) {
        return (root, query, builder) -> builder.equal(builder.upper(metaclassFunction.apply(root).as(String.class)), value.toUpperCase(Locale.FRANCE));
    }

    protected <F extends Comparable<? super F>> Specification<E> equalsDateSpecification(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        if (value instanceof ZonedDateTime) {
            ZonedDateTime valuePlusOneDay = ((ZonedDateTime) value).plusDays(1);
            return (root, query, builder) -> builder.and(builder.greaterThanOrEqualTo(metaclassFunction.apply(root), value), builder.lessThan(metaclassFunction.apply(root), (F) valuePlusOneDay));
        } else if (value instanceof Instant) {
            long oneDay = 60 * 60 * 24;
            Instant valuePlusOneDay = ((Instant) value).plusSeconds(oneDay);
            return (root, query, builder) -> builder.and(builder.greaterThanOrEqualTo(metaclassFunction.apply(root), value), builder.lessThan(metaclassFunction.apply(root), (F) valuePlusOneDay));
        } else {
            // LocalDate
            LocalDate valuePlusOneDay = ((LocalDate) value).plusDays(1);
            return (root, query, builder) -> builder.and(builder.greaterThanOrEqualTo(metaclassFunction.apply(root), value), builder.lessThan(metaclassFunction.apply(root), (F) valuePlusOneDay));
        }
    }

    /**
     * Generic method, which based on a Root&lt;E&gt; returns an Expression which type is the same as the given 'value' type.
     *
     * @param metaclassFunction function which returns the column which is used for filtering.
     * @param value             the actual value to exclude for.
     * @param <F>               The type of the attribute which is filtered.
     * @return a Specification.
     */
    protected <F extends Comparable<? super F>> Specification<E> notEqualsSpecification(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.not(builder.equal(metaclassFunction.apply(root), value));
    }

    protected <F extends Comparable<? super F>> Specification<E> notEqualsStringSpecification(Function<Root<E>, Expression<F>> metaclassFunction, String value) {
        return (root, query, builder) -> builder.not(builder.equal(builder.upper(metaclassFunction.apply(root).as(String.class)), value.toUpperCase(Locale.FRANCE)));
    }

    protected <F extends Comparable<? super F>> Specification<E> notEqualsDateSpecification(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.not(builder.and(builder.greaterThanOrEqualTo(metaclassFunction.apply(root), value), builder.lessThan(metaclassFunction.apply(root), value)));
    }

    /**
     * <p>likeUpperSpecification.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param value             a {@link java.lang.String} object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F extends Comparable<? super F>> Specification<E> likeUpperSpecification(Function<Root<E>, Expression<F>> metaclassFunction, final String value) {
        return (root, query, builder) -> builder.like(builder.upper(metaclassFunction.apply(root).as(String.class)), wrapLikeQuery(value));
    }

    protected <F extends Comparable<? super F>> Specification<E> startsWith(Function<Root<E>, Expression<F>> metaclassFunction, final String value) {
        return (root, query, builder) -> builder.like(builder.upper(metaclassFunction.apply(root).as(String.class)), wrapStartsWithQuery(value));
    }

    protected <F extends Comparable<? super F>> Specification<E> endsWith(Function<Root<E>, Expression<F>> metaclassFunction, final String value) {
        return (root, query, builder) -> builder.like(builder.upper(metaclassFunction.apply(root).as(String.class)), wrapEndsWithQuery(value));
    }

    protected <F extends Comparable<? super F>> Specification<E> likeUpperSpecificationIn(Function<Root<E>, Expression<F>> metaclassFunction, final Collection<String> values) {
        return (root, query, builder) -> {
            Predicate like = null;
            for (String value : values) {
                if (like == null) {
                    like = builder.like(builder.upper(metaclassFunction.apply(root).as(String.class)), wrapLikeQuery(value));
                } else {
                    like = builder.and(like, builder.like(builder.upper(metaclassFunction.apply(root).as(String.class)), wrapLikeQuery(value)));
                }
            }
            return like;
        };
    }

    protected <F extends Comparable<? super F>> Predicate likeUpperSpecificationUpperConcat(CriteriaBuilder builder, List<Expression<F>> expressionList, F value) {
        final Expression<String> stringExpression = concatUpperExpressions(builder, expressionList);
        return builder.like(stringExpression, wrapLikeQuery(value.toString()));
    }

    protected <F extends Comparable<? super F>> Expression<String> concatUpperExpressions(CriteriaBuilder builder, List<Expression<F>> expressionList) {
        return expressionList.stream().map(expression -> builder.coalesce(expression, "")).map(expression -> builder.upper(expression.as(String.class))).reduce(builder::concat).orElse(null);
    }

    /**
     * <p>notContainsSpecification.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param value             a {@link java.lang.String} object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F extends Comparable<? super F>> Specification<E> notContainsSpecification(Function<Root<E>, Expression<F>> metaclassFunction, final String value) {
        return (root, query, builder) -> builder.not(builder.like(builder.upper(metaclassFunction.apply(root).as(String.class)), wrapLikeQuery(value)));
    }

    /**
     * <p>byFieldSpecified.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param specified         a boolean.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F> Specification<E> byFieldSpecified(Function<Root<E>, Expression<F>> metaclassFunction, final boolean specified) {
        return specified ? (root, query, builder) -> builder.isNotNull(metaclassFunction.apply(root)) : (root, query, builder) -> builder.isNull(metaclassFunction.apply(root));
    }

    /**
     * <p>valueIn.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param values            a {@link java.util.Collection} object.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F> Specification<E> valueIn(Function<Root<E>, Expression<F>> metaclassFunction, final Collection<F> values) {
        return (root, query, builder) -> {
            In<F> in = builder.in(metaclassFunction.apply(root));
            for (F value : values) {
                in = in.value(value);
            }
            return in;
        };
    }

    /**
     * <p>valueNotIn.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param values            a {@link java.util.Collection} object.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F> Specification<E> valueNotIn(Function<Root<E>, Expression<F>> metaclassFunction, final Collection<F> values) {
        return (root, query, builder) -> {
            In<F> in = builder.in(metaclassFunction.apply(root));
            for (F value : values) {
                in = in.value(value);
            }
            return builder.not(in);
        };
    }

    /**
     * <p>greaterThanOrEqualTo.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param value             a F object.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F extends Comparable<? super F>> Specification<E> greaterThanOrEqualTo(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(metaclassFunction.apply(root), value);
    }

    /**
     * <p>greaterThan.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param value             a F object.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F extends Comparable<? super F>> Specification<E> greaterThan(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.greaterThan(metaclassFunction.apply(root), value);
    }

    /**
     * <p>lessThanOrEqualTo.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param value             a F object.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F extends Comparable<? super F>> Specification<E> lessThanOrEqualTo(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.lessThanOrEqualTo(metaclassFunction.apply(root), value);
    }

    /**
     * <p>lessThan.</p>
     *
     * @param metaclassFunction a {@link java.util.function.Function} object.
     * @param value             a F object.
     * @param <F>               a F object.
     * @return a {@link org.springframework.data.jpa.domain.Specification} object.
     */
    protected <F extends Comparable<? super F>> Specification<E> lessThan(Function<Root<E>, Expression<F>> metaclassFunction, final F value) {
        return (root, query, builder) -> builder.lessThan(metaclassFunction.apply(root), value);
    }

    /**
     * <p>wrapLikeQuery.</p>
     *
     * @param txt a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    protected String wrapLikeQuery(String txt) {
        return "%" + txt.toUpperCase() + '%';
    }

    protected String wrapStartsWithQuery(String txt) {
        return txt.toUpperCase() + '%';
    }

    protected String wrapEndsWithQuery(String txt) {
        return "%" + txt.toUpperCase();
    }

}
