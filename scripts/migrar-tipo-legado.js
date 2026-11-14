// Executar com mongosh, conectado ao banco do Thindesk, após backup.
// Repetível: documentos que já possuem "tipo" nunca são alterados.
const resultado = db.chamados.updateMany(
    { tipo: { $exists: false }, Tipo: { $type: "string" } },
    [{ $set: { tipo: { $trim: { input: "$Tipo" } } } }]
);
printjson({ encontrados: resultado.matchedCount, atualizados: resultado.modifiedCount });
