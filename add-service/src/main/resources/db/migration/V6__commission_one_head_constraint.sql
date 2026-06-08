CREATE UNIQUE INDEX uq_commission_one_head
    ON commission_member (commission_id)
    WHERE is_head = TRUE;
