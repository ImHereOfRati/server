#!/usr/bin/env node

import {readFile, writeFile} from 'node:fs/promises';
import {createHmac, randomUUID} from 'node:crypto';
import {fileURLToPath} from 'node:url';

const generatedDir = new URL('../generated/', import.meta.url);
const [
    fixturePath = fileURLToPath(new URL('fixture.json', generatedDir)),
    outputPath = fileURLToPath(new URL('tokens.json', generatedDir)),
] = process.argv.slice(2);

const appEnvPath = fileURLToPath(new URL('../../setup/test-env/server.env', import.meta.url));

const readEnvValue = async (path, key) => {
    const content = await readFile(path, 'utf8');
    for (const line of content.split(/\r?\n/)) {
        const trimmed = line.trim();
        if (!trimmed || trimmed.startsWith('#')) continue;
        const separator = trimmed.indexOf('=');
        if (separator < 0) continue;
        if (trimmed.slice(0, separator).trim() !== key) continue;
        return trimmed.slice(separator + 1).trim().replace(/^(['"])(.*)\1$/, '$2');
    }
    return '';
};

const secret = process.env.JWT_SECRET || (await readEnvValue(appEnvPath, 'JWT_SECRET'));
if (!secret) {
    throw new Error(`JWT_SECRET not found in ${appEnvPath}. Set it there or export JWT_SECRET.`);
}

const fixture = JSON.parse(await readFile(fixturePath, 'utf8'));
const now = Math.floor(Date.now() / 1000);
const encode = (value) => Buffer.from(JSON.stringify(value)).toString('base64url');
const sign = (value) => createHmac('sha256', secret).update(value).digest('base64url');

const tokens = fixture.users.map((user) => {
    const header = encode({alg: 'HS256', typ: 'JWT'});
    const payload = encode({
        jti: randomUUID(),
        category: 'access',
        uid: user.id,
        email: user.email,
        nickname: user.nickname,
        role: 'ROLE_NORMAL',
        status: 'ACTIVE',
        refreshTokenVersion: 0,
        iat: now,
        exp: now + 60 * 60 * 12,
    });
    const signingInput = `${header}.${payload}`;
    return {...user, accessToken: `${signingInput}.${sign(signingInput)}`};
});

await writeFile(outputPath, JSON.stringify({
    issuedAt: now,
    users: tokens,
    relations: fixture.relations
}, null, 2), 'utf8');
console.log(`Issued ${tokens.length} access tokens to ${outputPath}`);
