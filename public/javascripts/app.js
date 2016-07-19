$(function(){

    var url = 'api/parse';
    function loadTree() {
        var grammar = $('#grammar').val();
        var src = $('#src').val();
        var startRuleSel = $('#startRule');
        $.post(url, { grammar: grammar, src: src, rule: startRuleSel.val(), id: sessionId }, function(data){
            startRuleSel.empty();
            for(var o in data.rules) {
                startRuleSel.append('<option>' + data.rules[o] + '</option>');
            }

            draw(getTreeModel(data.tree));
            sessionId = data.id;
            $('#grammarError').hide();
        }).fail(function(err){
            $('#grammarError').show();
        });
    }

    var sessionId = location.hash.substring(1);
    if (sessionId) {
        loadTree();
    }

    function draw(tree) {
        $('#tree').jstree();
        $('#tree').jstree(true).settings.core.data = tree;
        $('#tree').jstree(true).refresh();
    }

    function getTreeModel(tree) {
        var children = [];
        for(var c in tree.children) {
            children.push(getTreeModel(tree.children[c]));
        }

        return { text: tree.rule + ': ' + tree.text, children: children, state: { opened: true } }
    }

    $('#parse').click(function() {
        loadTree();
    });
})