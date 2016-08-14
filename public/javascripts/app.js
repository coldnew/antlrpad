$(function(){

    var parseUrl = 'api/parse/';
    var loadUrl = 'api/load/';
    var saveUrl = 'api/save/';

    function loadRules(rules, rule) {
        var startRuleSel = $('#startRule');
        startRuleSel.empty();
        for(var o in rules) {
            startRuleSel.append('<option>' + rules[o] + '</option>');
        }

        startRuleSel.val(rule);
    }

    function showGrammarErrors(errors) {
        var errorNotifications = errors.map(function(e) {
        return {
            column: e.col,
            row: e.line - 1,
            type: e.errType,
            text: e.message
        }});

        editor.getSession().setAnnotations(errorNotifications);
    }

    function showParseMessages(messages) {
        if (messages.length == 0) {
            $('#parseMessages').hide();
        }
        else
        {
            var msg = messages.map(function(m) { return "line " + m.line + ", col: " + m.col + " : " + m.message; }).join("<br>");
            $('#parseMessages').text(msg).show();
        }
    }

    function parseExpression(url, callback) {
        var grammar = editor.getValue();
        var src = $('#src').val();
        var startRuleSel = $('#startRule');
        $.post(url, { grammar: grammar, src: src, rule: startRuleSel.val() }, function(data){
            if (data.tree && data.parsedGrammar.rules) {
                loadRules(data.parsedGrammar.rules, data.rule);
                draw(getTreeModel(data.tree));
                $('#grammarError').hide();
                showGrammarErrors(data.parsedGrammar.warnings);
                showParseMessages(data.messages);
            } else {
                $('#grammarError').show();
                showGrammarErrors(data.errors);
                draw(null);
            }

            if (callback) {
                callback(data.id);
            }

        }).fail(function(err){
            $('#grammarError').show();
        });
    }

    function loadTree(id) {
        $.get(loadUrl + id, {}, function(res){
            editor.setValue(res.grammar);
            $('#src').val(res.src);

            if (res.tree) {
                draw(getTreeModel(JSON.parse(res.tree)));
            }

            loadRules(res.rules.split(','), res.rule);
        });
    }

    var sessionId = location.hash.substring(1);
    if (sessionId) {
        loadTree(sessionId);
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

        var node = { text: tree.rule + ': ' + tree.text, children: children, state: { opened: true }, icon: false };
        if (tree.hasError) {
            node.icon = "glyphicon glyphicon-remove red";
        }

        return node;
    }

    $('#parse').click(function() {
        parseExpression(parseUrl);
    });

    $('#save').click(function() {
        parseExpression(saveUrl, function(id){
            window.location = '/#' + id;
        });
    });

    var reqTimer;
    $('#src').on('keyup', function() {
        if (reqTimer) {
            clearTimeout(reqTimer);
        }

        reqTimer = setTimeout(function(){
            parseExpression(parseUrl);
        }, 1000);
    });

    var editor = ace.edit("grammar");
    editor.setTheme("ace/theme/chrome");
    editor.getSession().setMode("ace/mode/antlr4");
})