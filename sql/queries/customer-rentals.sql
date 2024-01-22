-- :params {:id :long}
-- :defaults {:id 42}
-- :param-order [:id]
SELECT
    c.xt$id customer_id,
    f.title title,
    r.xt$valid_from rental_date,
    f.xt$id film_id,
    r.inventory_id inventory_id
FROM customer c
JOIN rental r ON r.customer_id = c.xt$id
JOIN inventory i ON i.xt$id = r.inventory_id
JOIN film f ON f.xt$id = i.film_id
WHERE c.xt$id = ?
