-- Catégories de départ (gérées par l'administrateur à partir de la v1.0)
INSERT INTO categories (name) VALUES
    ('Mariage'),
    ('Événementiel'),
    ('Sport'),
    ('Produit'),
    ('Clip'),
    ('Portrait')
ON CONFLICT (name) DO NOTHING;
