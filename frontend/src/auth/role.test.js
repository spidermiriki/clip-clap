import { describe, expect, it } from 'vitest'
import { roleFromProfile, routeForRole } from './role'

describe('roleFromProfile', () => {
  it("renvoie null si aucun claim de rôles n'est présent", () => {
    expect(roleFromProfile({ sub: '123' })).toBeNull()
  })

  it('renvoie null si le profil est undefined', () => {
    expect(roleFromProfile(undefined)).toBeNull()
  })

  it('lit le rôle depuis le claim générique', () => {
    const profile = { 'urn:zitadel:iam:org:project:roles': { CLIENT: {} } }
    expect(roleFromProfile(profile)).toBe('CLIENT')
  })

  it('lit le rôle depuis le claim propre à un projet', () => {
    const profile = { 'urn:zitadel:iam:org:project:395951731327930510:roles': { CREATOR: {} } }
    expect(roleFromProfile(profile)).toBe('CREATOR')
  })

  it('donne la priorité à ADMIN si plusieurs rôles sont présents', () => {
    const profile = { 'urn:zitadel:iam:org:project:roles': { CLIENT: {}, ADMIN: {} } }
    expect(roleFromProfile(profile)).toBe('ADMIN')
  })
})

describe('routeForRole', () => {
  it('renvoie /mon-profil pour CREATOR', () => {
    expect(routeForRole('CREATOR')).toBe('/mon-profil')
  })

  it.each(['CLIENT', 'ADMIN', null, undefined])('renvoie / pour le rôle %s', (role) => {
    expect(routeForRole(role)).toBe('/')
  })
})
