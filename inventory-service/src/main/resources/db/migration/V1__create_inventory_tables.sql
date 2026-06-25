CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX idx_inventory_product_id ON inventory_items(product_id);
