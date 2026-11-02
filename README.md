# Messagerie instantanée
Deux clients web, bureau et mobile, utilisent le même serveur Spring Boot et la même base MySQL. Le serveur attribue l’expéditeur et l’horodatage à chaque message.

## Démarrage sous Windows
Installer Java 17, Maven et MySQL Server 8. Démarrer le service MySQL. Dans PowerShell, depuis la racine du dépôt, exécuter `.\Lancer-Messagerie.ps1`. Le premier lancement local utilise le compte root pour créer la base et un utilisateur de développement. Les identifiants applicatifs sont ensuite conservés chiffrés dans le profil Windows. Pour une base déjà provisionnée, définir MESSAGERIE_DB_URL, MESSAGERIE_DB_USER et MESSAGERIE_DB_PASSWORD avant le lancement. MESSAGERIE_JWT_SECRET peut contenir une clé Base64 d’au moins 32 octets. Sans ce réglage, les jetons expirent aussi au redémarrage.

Ouvrir http://localhost:8080/ sur le PC et choisir Bureau. Sur un téléphone relié au même réseau, remplacer localhost par l’IPv4 du PC (commande ipconfig) et choisir Mobile. Le port 8080 doit être accessible depuis le réseau privé. Utiliser HTTPS et WSS avec un certificat valide hors du test local. Les fichiers frontend sont intégrés au serveur lors du build Maven.

## Démonstration
1. Créer deux comptes distincts, un par appareil. Attendre que les contacts apparaissent ou ouvrir la conversation.
2. Écrire depuis le téléphone. Vérifier l’indicateur de saisie sur le bureau puis envoyer. Répondre depuis le bureau.
3. Déconnecter le téléphone. Envoyer un autre message depuis le bureau, puis reconnecter le téléphone et vérifier l’historique.
4. Photographier les deux appareils avec un même message visible pour le rapport.

## API
| Action | Transport |
|---|---|
| Créer un compte avec username et motDePasse | POST /api/comptes |
| Obtenir le JWT | POST /api/auth/login |
| Consulter son compte | GET /api/comptes/me |
| Lister les utilisateurs et leur présence | GET /api/utilisateurs |
| Lire une conversation avec le correspondant choisi | GET /api/messages/{utilisateurId} |
| Messages et indicateurs de saisie | WebSocket /ws/messages?token=JWT |

Les GET attendent Authorization: Bearer JWT. Les clients transmettent type, destinataireId et contenu pour un message. Le serveur renvoie aussi id, expediteurId et dateEnvoi. Un événement typing est temporaire. Les connexions hors origine sont refusées par défaut. Éviter la journalisation des paramètres du handshake contenant le jeton.

## Tests
Depuis PowerShell, exécuter `.\Lancer-Messagerie.ps1 -Test` avec MySQL disponible. Les comptes créés par les tests d’intégration sont temporaires.
