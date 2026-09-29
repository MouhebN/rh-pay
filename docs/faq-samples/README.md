# Fichiers FAQ de test (upload admin)

Ces fichiers ne sont **pas** chargés tout seuls. Uploadez-les dans l’écran FAQ admin (md, catégorie indiquée).

Puis **rouvrez le widget** (l’index RAG se recharge après l’upload).

| Fichier | Catégorie à choisir | Questions de test |
|---|---|---|
| `teletravail.md` | **Congés (LEAVE)** | voir ci-dessous |
| `charte-materiel.md` | **Général (GENERAL)** | voisins plus faibles possibles |
| `mutuelle.md` | **Général** | souvent abstention / hors sujet si le classifieur ne passe pas en FAQ |

## Télétravail (après upload LEAVE)

- `Comment fonctionne le télétravail ?`
- `Quels jours sont autorisés en télétravail ?`
- `Faut-il poser un congé payé pour du télétravail ?`
- `Qui valide une demande de télétravail ?`

Attendu : pastille **Guide**, phrases du fichier (2 jours, lundi/mercredi, pas un congé payé).

Si vous retrouvez encore « Mes demandes de congé / ENATTENTE », le retrieval a préféré `conges.md`. Reformulez avec le mot **télétravail**.

## Matériel

- `Comment déclarer un PC portable défectueux ?`

## Mutuelle (contrôle négatif)

- `Quel est le taux de remboursement mutuelle dentaire ?`

Attendu possible : hors sujet ou « pas trouvé dans le guide » — le classifieur n’a pas de classe mutuelle.
