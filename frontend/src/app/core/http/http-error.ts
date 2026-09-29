import { HttpErrorResponse } from '@angular/common/http';

const ENGLISH_MESSAGES: Record<string, string> = {
  'employee email already exists': 'Cette adresse e-mail est déjà utilisée.',
  'employee matricule already exists': 'Ce matricule est déjà utilisé.',
  'email already exists': 'Cette adresse e-mail est déjà utilisée.',
  'username already exists': 'Ce nom d’utilisateur est déjà utilisé.',
  'department already exists': 'Ce département existe déjà.',
  'job position already exists': 'Ce poste existe déjà.',
  'employee base salary cannot be negative': 'Le salaire de base ne peut pas être négatif.',
  'contract salary is required when employee base salary is missing':
    'Indiquez le salaire du contrat lorsque le salaire de base est vide.',
  'an overlapping leave request already exists': 'Une demande de congé se chevauche déjà sur cette période.',
  'insufficient leave balance': 'Solde de congé insuffisant.',
  'rejection reason is required': 'Le motif de refus est obligatoire.',
  'only pending leave requests can be modified': 'Seules les demandes en attente peuvent être modifiées.',
  'employee is required': 'Un employé est obligatoire.',
  'start date must be before or equal to end date': 'La date de début doit précéder ou égaler la date de fin.',
  'document is required': 'Un document est obligatoire.',
  'document size must not exceed 5mb': 'Le document ne doit pas dépasser 5 Mo.',
  'only pdf, png, jpg and jpeg files are accepted': 'Formats acceptés : PDF, PNG, JPG/JPEG.',
  'only pdf files are accepted': 'Seul le format PDF est accepté.',
  'invalid file name': 'Nom de fichier invalide.',
  'document not found': 'Document introuvable.',
  'document file not found': 'Fichier du document introuvable.',
  'this employee already has an active exit process': 'Cet employé a déjà une sortie en cours.',
  'employee is already inactive or has left': 'Cet employé est déjà inactif ou a quitté l’entreprise.',
  'exit is already cancelled': 'Cette sortie est déjà annulée.',
  'only draft or scheduled exits can be updated': 'Seules les sorties brouillon ou planifiées peuvent être modifiées.',
  'only draft or scheduled exits can be completed': 'Seules les sorties brouillon ou planifiées peuvent être clôturées.',
  'exit status must be draft or scheduled': 'Le statut de la sortie doit être brouillon ou planifié.',
  'cannot edit terminated, expired, or renewed contracts':
    'Impossible de modifier un contrat résilié, expiré ou renouvelé.',
  'only draft contracts can be activated': 'Seuls les contrats brouillon peuvent être activés.',
  'contract is already closed': 'Ce contrat est déjà clôturé.',
  'only active or expired contracts can be renewed': 'Seuls les contrats actifs ou expirés peuvent être renouvelés.',
  'only draft contracts can be deleted': 'Seuls les contrats brouillon peuvent être supprimés.',
  'contract document not found': 'Document du contrat introuvable.',
  'contract document file not found': 'Fichier du contrat introuvable.',
  'payslip is cancelled': 'Ce bulletin est annulé.',
  'payslip requires a finalized payroll calculation': 'Le bulletin nécessite une paie finalisée.',
  'payslip file not found': 'Fichier du bulletin introuvable.',
  'you cannot access this contract document': 'Vous ne pouvez pas accéder à ce document de contrat.',
  'you cannot access this leave request': 'Vous ne pouvez pas accéder à cette demande de congé.',
  'you cannot cancel this leave request': 'Vous ne pouvez pas annuler cette demande de congé.',
  'you cannot modify this document': 'Vous ne pouvez pas modifier ce document.',
  'you cannot access this document': 'Vous ne pouvez pas accéder à ce document.',
  'invalid username or password': 'Vérifiez le nom d’utilisateur et le mot de passe.',
  'invalid refresh token': 'Session expirée, veuillez vous reconnecter.',
  'refresh token expired': 'Session expirée, veuillez vous reconnecter.',
  'validation failed': 'Certaines informations sont invalides.',
  'certaines informations sont invalides.': 'Certaines informations sont invalides.',
  'request failed': 'La requête a échoué.',
  'la requête a échoué.': 'La requête a échoué.',
  'must not be blank': 'Ce champ est obligatoire.',
  'must not be null': 'Ce champ est obligatoire.',
  'must not be empty': 'Ce champ est obligatoire.',
  'must be a well-formed email address': 'Adresse e-mail invalide.',
  'unable to store employee exit document': 'Impossible d’enregistrer le document de sortie.',
  'unable to store contract document': 'Impossible d’enregistrer le document du contrat.',
  'unable to store payslip pdf': 'Impossible d’enregistrer le bulletin.',
  'unable to read document': 'Impossible de lire le document.',
  'unable to read contract document': 'Impossible de lire le document du contrat.',
  'unable to delete document file': 'Impossible de supprimer le document.',
  'unable to delete existing contract document': 'Impossible de supprimer le document du contrat.',
  'access denied': 'Vous n’avez pas les droits pour cette action.',
  'forbidden': 'Vous n’avez pas les droits pour cette action.',
  'unauthorized': 'Session expirée, veuillez vous reconnecter.',
  'conflict': 'Cette donnée est en conflit avec un enregistrement existant.',
  'not found': 'Ressource introuvable.',
  'bad request': 'Les données envoyées sont invalides.',
  'internal server error': 'Une erreur interne est survenue.'
};

const notifiedErrors = new WeakSet<object>();

export function markHttpErrorNotified(error: unknown): void {
  if (typeof error === 'object' && error !== null) {
    notifiedErrors.add(error);
  }
}

export function wasHttpErrorNotified(error: unknown): boolean {
  return typeof error === 'object' && error !== null && notifiedErrors.has(error);
}

export function httpErrorTitle(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Action impossible';
  }

  if (isLoginRequest(error)) {
    return 'Connexion refusée';
  }

  switch (error.status) {
    case 0:
      return 'Connexion impossible';
    case 400:
    case 422:
      return 'Données invalides';
    case 401:
      return 'Session';
    case 403:
      return 'Accès refusé';
    case 404:
      return 'Introuvable';
    case 409:
      return 'Conflit';
    default:
      return error.status >= 500 ? 'Erreur serveur' : 'Action impossible';
  }
}

export function httpErrorFallback(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Une erreur inattendue est survenue.';
  }

  if (isLoginRequest(error)) {
    return 'Vérifiez le nom d’utilisateur et le mot de passe.';
  }

  switch (error.status) {
    case 0:
      return 'Impossible de contacter le serveur.';
    case 400:
    case 422:
      return 'Les données envoyées sont invalides.';
    case 401:
      return 'Session expirée, veuillez vous reconnecter.';
    case 403:
      return 'Vous n’avez pas les droits pour cette action.';
    case 404:
      return 'Ressource introuvable.';
    case 409:
      return 'Cette donnée est en conflit avec un enregistrement existant.';
    default:
      return error.status >= 500
        ? 'Une erreur interne est survenue.'
        : 'L’action n’a pas pu être effectuée.';
  }
}

export function extractErrorMessage(error: unknown, fallback: string): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  const parsed = parseErrorBody(error.error);
  const validationDetails = validationMessages(parsed);
  const rawMessage = firstNonEmpty(
    validationDetails,
    readString(parsed, 'message'),
    readString(parsed, 'detail'),
    readString(parsed, 'error'),
    readString(parsed, 'title'),
    typeof parsed === 'string' ? parsed : ''
  );

  const translated = translateMessage(rawMessage);
  if (translated) {
    return translated;
  }

  return fallback || httpErrorFallback(error);
}

export function translateMessage(message: string | null | undefined): string {
  if (!message) {
    return '';
  }

  const trimmed = message.trim();
  if (!trimmed || looksLikeHtml(trimmed)) {
    return '';
  }

  return ENGLISH_MESSAGES[normalizeKey(trimmed)] ?? trimmed;
}

function parseErrorBody(body: unknown): unknown {
  if (body == null || body === '') {
    return null;
  }

  if (typeof body === 'string') {
    const trimmed = body.trim();
    if (!trimmed || looksLikeHtml(trimmed)) {
      return null;
    }
    if (trimmed.startsWith('{') || trimmed.startsWith('[')) {
      try {
        return JSON.parse(trimmed);
      } catch {
        return trimmed;
      }
    }
    return trimmed;
  }

  return body;
}

function validationMessages(body: unknown): string {
  if (!isRecord(body)) {
    return '';
  }

  const data = body['data'];
  if (!Array.isArray(data) || data.length === 0) {
    return '';
  }

  const details = data
    .map((item) => (typeof item === 'string' ? translateMessage(cleanFieldError(item)) : ''))
    .filter(Boolean);

  return details.join('\n');
}

function cleanFieldError(value: string): string {
  return value.replace(/^[a-zA-Z0-9_.]+:\s*/, '');
}

function readString(body: unknown, key: string): string {
  if (!isRecord(body)) {
    return '';
  }
  const value = body[key];
  return typeof value === 'string' ? value : '';
}

function firstNonEmpty(...values: Array<string | null | undefined>): string {
  return values.find((value) => Boolean(value && value.trim()))?.trim() ?? '';
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function looksLikeHtml(value: string): boolean {
  return value.startsWith('<') && /<\/?[a-z][\s\S]*>/i.test(value);
}

function normalizeKey(value: string): string {
  return value.trim().toLowerCase().replace(/\s+/g, ' ');
}

function isLoginRequest(error: HttpErrorResponse): boolean {
  return Boolean(error.url?.includes('/api/auth/login'));
}
