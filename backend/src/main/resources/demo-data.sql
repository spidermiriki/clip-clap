-- Données de démonstration (#18), chargées uniquement avec le profil Spring « demo ».
-- Rejouable à chaque démarrage : ON CONFLICT DO NOTHING ou NOT EXISTS, identifiants retrouvés par e-mail.
-- Comptes fictifs : domaine réservé .test et sub sans compte Zitadel, donc aucune connexion possible.
-- Albums et photos de départ (#15) : 4 créateurs ont un album de 2 photos (pas de vidéo avant la v0.2, #94).
-- Prestations et créneaux (#141) : Farid et Alice, créneaux sur les 3 semaines qui suivent la date du démarrage.

-- Module users : 10 créateurs visibles, 1 compte suspendu
INSERT INTO users (email, sub, role, suspended) VALUES
    ('alice.martin@clicclap.test',    'demo-creator-01', 'CREATOR', FALSE),
    ('bruno.lambert@clicclap.test',   'demo-creator-02', 'CREATOR', FALSE),
    ('chloe.dubois@clicclap.test',    'demo-creator-03', 'CREATOR', FALSE),
    ('david.renard@clicclap.test',    'demo-creator-04', 'CREATOR', FALSE),
    ('emma.fontaine@clicclap.test',   'demo-creator-05', 'CREATOR', FALSE),
    ('farid.benali@clicclap.test',    'demo-creator-06', 'CREATOR', FALSE),
    ('gaelle.simon@clicclap.test',    'demo-creator-07', 'CREATOR', FALSE),
    ('hugo.petit@clicclap.test',      'demo-creator-08', 'CREATOR', FALSE),
    ('ines.moreau@clicclap.test',     'demo-creator-09', 'CREATOR', FALSE),
    ('julien.leroy@clicclap.test',    'demo-creator-10', 'CREATOR', FALSE),
    ('compte.suspendu@clicclap.test', 'demo-creator-11', 'CREATOR', TRUE),
    ('profil.attente@clicclap.test',  'demo-creator-12', 'CREATOR', FALSE)
ON CONFLICT (email) DO NOTHING;

-- Module creators : profils (le suspendu et le profil en attente ne sont visibles nulle part)
INSERT INTO creator_profiles (user_id, display_name, bio, city, country, service_areas, validation_status)
SELECT u.id, p.display_name, p.bio, p.city, p.country, p.service_areas, p.status
FROM (VALUES
    ('alice.martin@clicclap.test',    'Alice Martin',    'Photographe de mariage depuis dix ans, reportage naturel et lumineux.', 'Mons',      'Belgique', 'Hainaut, Bruxelles',          'VALIDATED'),
    ('bruno.lambert@clicclap.test',   'Bruno Lambert',   'Vidéaste événementiel : soirées, conférences et lancements de produit.', 'Bruxelles', 'Belgique', 'Bruxelles, Brabant wallon',  'VALIDATED'),
    ('chloe.dubois@clicclap.test',    'Chloé Dubois',    'Portraits en studio ou en extérieur, particuliers et entreprises.',     'Liège',     'Belgique', 'Province de Liège',          'VALIDATED'),
    ('david.renard@clicclap.test',    'David Renard',    'Photographe sportif : clubs amateurs, courses et tournois.',            'Namur',     'Belgique', 'Namur, Luxembourg',          'VALIDATED'),
    ('emma.fontaine@clicclap.test',   'Emma Fontaine',   'Photos de produits pour boutiques en ligne et catalogues.',             'Charleroi', 'Belgique', 'Hainaut',                    'VALIDATED'),
    ('farid.benali@clicclap.test',    'Farid Benali',    'Réalisateur de clips musicaux et de contenus pour les réseaux.',        'Mons',      'Belgique', 'Hainaut, Lille',             'VALIDATED'),
    ('gaelle.simon@clicclap.test',    'Gaëlle Simon',    'Mariages et portraits de famille, en Belgique comme en France.',        'Lille',     'France',   'Hauts-de-France, Hainaut',   'VALIDATED'),
    ('hugo.petit@clicclap.test',      'Hugo Petit',      'Couverture photo et vidéo de salons, galas et séminaires.',             'Paris',     'France',   'Île-de-France',              'VALIDATED'),
    ('ines.moreau@clicclap.test',     'Inès Moreau',     'Photographe de sport en extérieur : trail, cyclisme, sports nautiques.', 'Lyon',      'France',   'Auvergne-Rhône-Alpes',       'VALIDATED'),
    ('julien.leroy@clicclap.test',    'Julien Leroy',    'Packshots et mises en scène de produits, retouche incluse.',            'Bordeaux',  'France',   'Nouvelle-Aquitaine',         'VALIDATED'),
    ('compte.suspendu@clicclap.test', 'Compte Suspendu', 'Compte suspendu : absent de la galerie et du profil public.',           'Mons',      'Belgique', 'Hainaut',                    'VALIDATED'),
    ('profil.attente@clicclap.test',  'Profil En Attente', 'Profil pas encore validé : absent de la galerie et du profil public.', 'Mons',      'Belgique', 'Hainaut',                    'PENDING')
) AS p (email, display_name, bio, city, country, service_areas, status)
JOIN users u ON u.email = p.email
ON CONFLICT (user_id) DO NOTHING;

-- Module creators : types de prestation (les 6 catégories de data.sql sont couvertes)
INSERT INTO creator_categories (creator_id, category_id)
SELECT u.id, c.id
FROM (VALUES
    ('alice.martin@clicclap.test',    'Mariage'),
    ('alice.martin@clicclap.test',    'Portrait'),
    ('bruno.lambert@clicclap.test',   'Événementiel'),
    ('bruno.lambert@clicclap.test',   'Produit'),
    ('chloe.dubois@clicclap.test',    'Portrait'),
    ('david.renard@clicclap.test',    'Sport'),
    ('emma.fontaine@clicclap.test',   'Produit'),
    ('farid.benali@clicclap.test',    'Clip'),
    ('farid.benali@clicclap.test',    'Événementiel'),
    ('gaelle.simon@clicclap.test',    'Mariage'),
    ('gaelle.simon@clicclap.test',    'Portrait'),
    ('hugo.petit@clicclap.test',      'Événementiel'),
    ('ines.moreau@clicclap.test',     'Sport'),
    ('julien.leroy@clicclap.test',    'Produit'),
    ('compte.suspendu@clicclap.test', 'Mariage'),
    ('profil.attente@clicclap.test',  'Mariage')
) AS l (email, category)
JOIN users u ON u.email = l.email
JOIN categories c ON c.name = l.category
ON CONFLICT DO NOTHING;

-- Module creators : albums, photos (#15) et une vidéo (#94) de départ. Images de la médiathèque Cloudinary du projet
-- (URL de diffusion publiques : seul le nom du cloud y figure, aucun identifiant secret).
-- Pas de clé unique sur albums et media : NOT EXISTS évite les doublons quand le script est rejoué.
INSERT INTO albums (creator_id, name)
SELECT u.id, a.name
FROM (VALUES
    ('alice.martin@clicclap.test',  'Mariages 2026'),
    ('bruno.lambert@clicclap.test', 'Soirées et événements'),
    ('chloe.dubois@clicclap.test',  'Portraits animaliers'),
    ('ines.moreau@clicclap.test',   'Nature et grand air'),
    ('farid.benali@clicclap.test',  'Clips musicaux')
) AS a (email, name)
JOIN users u ON u.email = a.email
WHERE NOT EXISTS (SELECT 1 FROM albums x WHERE x.creator_id = u.id AND x.name = a.name);

-- ORDER BY m.ordre : les jointures ne garantissent pas l'ordre des lignes, donc celui des photos dans l'album.
-- La vidéo d'exemple (#94) a une URL sans version, servie telle quelle par Cloudinary.
INSERT INTO media (album_id, url, cloudinary_public_id, media_type)
SELECT al.id, m.url, m.public_id, m.media_type
FROM (VALUES
    (1, 'alice.martin@clicclap.test',  'Mariages 2026',         'samples/two-ladies',         'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471943/samples/two-ladies.jpg', 'PHOTO'),
    (2, 'alice.martin@clicclap.test',  'Mariages 2026',         'samples/smile',              'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471945/samples/smile.jpg', 'PHOTO'),
    (3, 'bruno.lambert@clicclap.test', 'Soirées et événements', 'samples/balloons',           'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471944/samples/balloons.jpg', 'PHOTO'),
    (4, 'bruno.lambert@clicclap.test', 'Soirées et événements', 'samples/imagecon-group',     'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471940/samples/imagecon-group.jpg', 'PHOTO'),
    (5, 'chloe.dubois@clicclap.test',  'Portraits animaliers',  'samples/animals/cat',        'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471935/samples/animals/cat.jpg', 'PHOTO'),
    (6, 'chloe.dubois@clicclap.test',  'Portraits animaliers',  'samples/animals/three-dogs', 'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471938/samples/animals/three-dogs.jpg', 'PHOTO'),
    (7, 'ines.moreau@clicclap.test',   'Nature et grand air',   'samples/sheep',              'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471937/samples/sheep.jpg', 'PHOTO'),
    (8, 'ines.moreau@clicclap.test',   'Nature et grand air',   'samples/animals/reindeer',   'https://res.cloudinary.com/mcnayzr8/image/upload/v1794471936/samples/animals/reindeer.jpg', 'PHOTO'),
    (9, 'farid.benali@clicclap.test',  'Clips musicaux',        'samples/cld-sample-video',   'https://res.cloudinary.com/mcnayzr8/video/upload/samples/cld-sample-video.mp4', 'VIDEO')
) AS m (ordre, email, album, public_id, url, media_type)
JOIN users u ON u.email = m.email
JOIN albums al ON al.creator_id = u.id AND al.name = m.album
WHERE NOT EXISTS (SELECT 1 FROM media x WHERE x.album_id = al.id AND x.cloudinary_public_id = m.public_id)
ORDER BY m.ordre;

-- Module offerings : prestations de Farid et d'Alice (#141), catégories de data.sql retrouvées par leur nom.
-- Pas de clé unique sur offerings ni offering_options : NOT EXISTS évite les doublons quand le script est rejoué.
INSERT INTO offerings (creator_id, name, duration_minutes, base_price, category_id)
SELECT u.id, o.name, o.duration, o.price, c.id
FROM (VALUES
    ('farid.benali@clicclap.test', 'Clip musical',            180,  900.00, 'Clip'),
    ('farid.benali@clicclap.test', 'Teaser réseaux sociaux',   60,  250.00, 'Clip'),
    ('farid.benali@clicclap.test', 'Captation de concert',    240,  600.00, 'Événementiel'),
    ('alice.martin@clicclap.test', 'Reportage mariage',       480, 1200.00, 'Mariage'),
    ('alice.martin@clicclap.test', 'Séance portrait',          90,  150.00, 'Portrait')
) AS o (email, name, duration, price, category)
JOIN users u ON u.email = o.email
JOIN categories c ON c.name = o.category
WHERE NOT EXISTS (SELECT 1 FROM offerings x WHERE x.creator_id = u.id AND x.name = o.name);

INSERT INTO offering_options (offering_id, name, extra_price)
SELECT f.id, op.name, op.price
FROM (VALUES
    ('farid.benali@clicclap.test', 'Clip musical',           'Montage express 48 h',     150.00),
    ('farid.benali@clicclap.test', 'Clip musical',           'Prises de vue par drone',  200.00),
    ('farid.benali@clicclap.test', 'Teaser réseaux sociaux', 'Format vertical en plus',   50.00),
    ('alice.martin@clicclap.test', 'Reportage mariage',      'Album imprimé',            250.00),
    ('alice.martin@clicclap.test', 'Reportage mariage',      'Second photographe',       300.00)
) AS op (email, offering, name, price)
JOIN users u ON u.email = op.email
JOIN offerings f ON f.creator_id = u.id AND f.name = op.offering
WHERE NOT EXISTS (SELECT 1 FROM offering_options x WHERE x.offering_id = f.id AND x.name = op.name);

-- Module offerings : créneaux libres de J+1 à J+21 (#141), calculés à partir de CURRENT_DATE pour ne jamais
-- périmer. Heures locales de Bruxelles (changement d'heure compris). Rejoué un autre jour, le script n'ajoute
-- que les jours qui manquent en fin de fenêtre, et les créneaux passés ne sont plus proposés à la réservation.
INSERT INTO availability_slots (creator_id, starts_at, ends_at)
SELECT u.id,
       ((CURRENT_DATE + j.n) + d.debut) AT TIME ZONE 'Europe/Brussels',
       ((CURRENT_DATE + j.n) + d.fin) AT TIME ZONE 'Europe/Brussels'
FROM (VALUES
    -- jour ISO : 1 = lundi … 7 = dimanche
    ('farid.benali@clicclap.test', 2, TIME '14:00', TIME '18:00'),
    ('farid.benali@clicclap.test', 4, TIME '14:00', TIME '18:00'),
    ('farid.benali@clicclap.test', 6, TIME '10:00', TIME '18:00'),
    ('alice.martin@clicclap.test', 3, TIME '10:00', TIME '12:00'),
    ('alice.martin@clicclap.test', 6, TIME '10:00', TIME '20:00')
) AS d (email, jour, debut, fin)
JOIN users u ON u.email = d.email
CROSS JOIN generate_series(1, 21) AS j (n)
WHERE EXTRACT(ISODOW FROM CURRENT_DATE + j.n) = d.jour
  AND NOT EXISTS (
      SELECT 1 FROM availability_slots x
      WHERE x.creator_id = u.id
        AND x.starts_at = ((CURRENT_DATE + j.n) + d.debut) AT TIME ZONE 'Europe/Brussels');
